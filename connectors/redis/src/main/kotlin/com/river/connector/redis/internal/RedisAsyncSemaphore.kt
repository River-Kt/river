package com.river.connector.redis.internal

import com.river.core.AsyncSemaphore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.redisson.api.RPermitExpirableSemaphore
import java.util.concurrent.TimeUnit.MILLISECONDS
import kotlin.time.Duration
import kotlinx.coroutines.future.await as coAwait

/**
 * A Redis-backed implementation of the [AsyncSemaphore] interface.
 * This class uses a [RPermitExpirableSemaphore] to manage the permits in a distributed environment.
 * Permits are released automatically after a given lease time.
 */
internal class RedisAsyncSemaphore(
    override val totalPermits: Int,
    val scope: CoroutineScope,
    val leaseTime: Duration,
    private val rSemaphore: RPermitExpirableSemaphore
) : AsyncSemaphore {
    private val mutex = Mutex()
    private val acquired: MutableMap<String, Job> = mutableMapOf()

    override suspend fun available(): Int =
        rSemaphore.availablePermitsAsync().coAwait()

    override suspend fun acquire(): String {
        val id =
            rSemaphore
                .acquireAsync(leaseTime.inWholeMilliseconds, MILLISECONDS)
                .coAwait()

        mutex.withLock {
            acquired[id] = scope.launch {
                delay(leaseTime)
                release(id)
            }
        }

        return id
    }

    override suspend fun tryAcquire(): String? {
        val id =
            rSemaphore
                .tryAcquireAsync(0, leaseTime.inWholeMilliseconds, MILLISECONDS)
                .coAwait()

        id?.also {
            mutex.withLock {
                acquired[id] = scope.launch {
                    delay(leaseTime)
                    mutex.withLock { acquired.remove(id) }
                }
            }
        }

        return id
    }

    override suspend fun release(permit: String) {
        rSemaphore.releaseAsync(permit).coAwait()

        mutex.withLock {
            acquired[permit]?.cancel()
            acquired.remove(permit)
        }
    }

    override suspend fun releaseAll() {
        acquired.forEach { (key, _) -> release(key) }
    }
}
