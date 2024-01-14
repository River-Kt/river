package com.river.core.internal

import com.river.core.AsyncSemaphore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration

/**
 * The default implementation of the [AsyncSemaphore] interface.
 * This class uses the [Semaphore] from kotlinx.coroutines.sync package to manage the permits.
 */
internal class DefaultAsyncSemaphore(
    private val scope: CoroutineScope,
    override val totalPermits: Int,
    private val leaseTime: Duration? = null,
) : AsyncSemaphore<Int> {
    private val mutex = Mutex()
    private val internal = Semaphore(totalPermits)

    private val permits: Array<SimplePermit> =
        (0 until totalPermits)
            .map { SimplePermit(it, SimplePermit.State.Available) }
            .toTypedArray()

    private val available
        get() = permits.filter { it.state is SimplePermit.State.Available }

    private val nextAvailable
        get() = available.first()

    class SimplePermit(
        val id: Int,
        var state: State
    ) {
        sealed interface State {
            class Acquired(val timeout: Job?): State
            object Available : State
        }
    }

    override suspend fun available(): Int =
        internal.availablePermits

    override suspend fun acquire(): Int {
        internal.acquire()
        val permit = nextAvailable

        permit.state = SimplePermit.State.Acquired(leaseTime?.let {
            mutex.withLock {
                scope.launch {
                    delay(leaseTime)
                    release(permit.id)
                }
            }
        })

        return permit.id
    }

    override suspend fun tryAcquire(): Int? {
        if (!internal.tryAcquire()) {
            return null
        }

        val permit = nextAvailable

        permit.state = SimplePermit.State.Acquired(leaseTime?.let {
            mutex.withLock {
                scope.launch {
                    delay(leaseTime)
                    release(permit.id)
                }
            }
        })

        return permit.id
    }

    override suspend fun release(permit: Int) {
        mutex.withLock {
            val p = permits[permit]

            when (val state = p.state) {
                is SimplePermit.State.Acquired -> {
                    state.timeout?.cancel()
                    permits[permit].state = SimplePermit.State.Available
                    internal.release()
                }

                SimplePermit.State.Available -> {
                }
            }
        }
    }

    override suspend fun releaseAll() =
       permits.forEach { release(it.id) }
}
