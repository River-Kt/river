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

    private val availablePermits: ArrayDeque<Int> =
        ArrayDeque((0 until totalPermits).toList())

    private val permits: Array<PermitState> =
        (0 until totalPermits)
            .map { PermitState.Available }
            .toTypedArray()

    sealed interface PermitState {
        class Acquired(val timeout: Job?) : PermitState
        data object Available : PermitState
    }

    override suspend fun available(): Int =
        internal.availablePermits

    override suspend fun acquire(): Int {
        internal.acquire()
        return internalAcquire()
    }

    override suspend fun tryAcquire(): Int? {
        if (!internal.tryAcquire()) {
            return null
        }

        return internalAcquire()
    }

    override suspend fun release(permit: Int) {
        when (val state = permits[permit]) {
            is PermitState.Acquired -> {
                state.timeout?.cancel()

                mutex.withLock {
                    permits[permit] = PermitState.Available
                    availablePermits.addLast(permit)
                }

                internal.release()
            }

            PermitState.Available -> {
            }
        }
    }

    override suspend fun releaseAll() =
        permits.forEachIndexed { index, _ -> release(index) }

    private suspend fun internalAcquire(): Int {
        val permit = mutex.withLock { availablePermits.removeFirst() }

        permits[permit] = PermitState.Acquired(leaseTime?.let {
            scope.launch {
                delay(leaseTime)
                release(permit)
            }
        })

        return permit
    }
}
