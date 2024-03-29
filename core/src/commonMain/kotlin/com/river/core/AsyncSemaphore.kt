package com.river.core

import com.river.core.internal.DefaultAsyncSemaphore
import kotlinx.coroutines.CoroutineScope
import kotlin.time.Duration

/**
 * An interface representing a non-blocking semaphore.
 *
 * A semaphore maintains a set of permits and each `acquire()` blocks if necessary until a permit is available,
 * and then takes it. Each `release()` adds a permit, potentially releasing an acquirer.
 */
interface AsyncSemaphore<P> {
    /**
     * The total number of permits this semaphore can provide.
     */
    val totalPermits: Int

    /**
     * The number of available permits.
     *
     * @return The number of available permits.
     */
    suspend fun available(): Int

    /**
     * Acquires a permit from this semaphore, suspending until one is available.
     */
    suspend fun acquire(): P

    /**
     * Tries to acquire a permit from this semaphore. This function is marked as a suspend function because it may
     * perform I/O operations, but it won't suspend in case that no permit is available at the moment.
     *
     * @return `permit` if a permit was acquired and `null` otherwise.
     */
    suspend fun tryAcquire(): P?

    /**
     * Releases a permit, returning it to the semaphore.
     */
    suspend fun release(permit: P)

    /**
     * Releases all permits back to the semaphore.
     */
    suspend fun releaseAll()

    companion object {
        /**
         * Returns an instance of an [AsyncSemaphore] with the specified number of permits.
         *
         * @param scope The number of permits this semaphore can provide.
         * @param permits The number of permits this semaphore can provide.
         * @return An [AsyncSemaphore] instance with the specified number of permits.
         */
        operator fun invoke(
            scope: CoroutineScope,
            permits: Int,
            leaseTime: Duration? = null
        ): AsyncSemaphore<Int> =
            DefaultAsyncSemaphore(scope, permits, leaseTime)
    }
}
