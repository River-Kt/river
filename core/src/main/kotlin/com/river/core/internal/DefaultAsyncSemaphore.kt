package com.river.core.internal

import com.river.core.AsyncSemaphore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import java.util.*
import kotlin.time.Duration

/**
 * The default implementation of the [AsyncSemaphore] interface.
 * This class uses the [Semaphore] from kotlinx.coroutines.sync package to manage the permits.
 */
internal class DefaultAsyncSemaphore(
    private val scope: CoroutineScope,
    override val totalPermits: Int,
    private val leaseTime: Duration? = null,
) : AsyncSemaphore {
    private val mutex = Mutex()
    private val acquired: MutableMap<String, Job?> = mutableMapOf()
    private val internal = Semaphore(totalPermits)

    override suspend fun available(): Int =
        internal.availablePermits

    override suspend fun acquire(): String {
        internal.acquire()
        val id = random

        acquired[id] = leaseTime?.let {
            mutex.withLock {
                scope.launch {
                    delay(leaseTime)
                    release(id)
                }
            }
        }

        return id
    }

    override suspend fun tryAcquire(): String? {
        if (!internal.tryAcquire()) {
            return null
        }

        val id = random

        acquired[id] = leaseTime?.let {
            mutex.withLock {
                scope.launch {
                    delay(leaseTime)
                    release(id)
                }
            }
        }

        return id
    }

    override suspend fun release(permit: String) {
        mutex.withLock {
            if (acquired.containsKey(permit)) {
                acquired[permit]?.cancel()
                internal.release()
                acquired.remove(permit)
            }
        }
    }

    override suspend fun releaseAll() =
        acquired.forEach { (key, _) -> release(key) }

    private val random: String
        get() = UUID.randomUUID().toString()
}
