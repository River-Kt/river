package com.river.core

import com.river.core.internal.ThrottleFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration

/**
 * Throttles the emission of elements from the [Flow] based on the specified [elementsPerInterval], [interval], and [strategy].
 *
 * @param elementsPerInterval The maximum number of elements allowed to be emitted per [interval].
 * @param interval A [Duration] specifying the time interval for throttling the flow.
 * @param strategy The [ThrottleStrategy] to apply when the flow exceeds the specified rate.
 *                 Defaults to [ThrottleStrategy.Suspend].
 *                 [ThrottleStrategy.Suspend] will suspend the emission until the next interval.
 *                 [ThrottleStrategy.Drop] will drop the element if the rate is exceeded.
 *
 * @return A new [Flow] with throttling applied based on the specified parameters.
 *
 * Example usage:
 *
 * ```
 *  flowOf(1, 2, 3, 4, 5)
 *      .throttle(elementsPerInterval = 2, interval = 1.seconds, strategy = ThrottleStrategy.Suspend)
 *  // Output: 1, 2, (1s delay), 3, 4, (1s delay), 5
 * ```
 */
fun <T> Flow<T>.throttle(
    elementsPerInterval: Int,
    interval: Duration,
    strategy: ThrottleStrategy = ThrottleStrategy.Suspend
): Flow<T> = throttle(strategy) { AsyncSemaphore(this, elementsPerInterval, interval) }

/**
 * Throttles the emission of elements from the [Flow] based on the specified [semaphore], [interval], and [strategy].
 *
 * @param strategy The [ThrottleStrategy] to apply when the flow exceeds the specified rate.
 *                 Defaults to [ThrottleStrategy.Suspend].
 *                 [ThrottleStrategy.Suspend] will suspend the emission until the next interval.
 *                 [ThrottleStrategy.Drop] will drop the element if the rate is exceeded.
 * @param semaphore The [AsyncSemaphore] that controls the emission of the elements.
 *
 * @return A new [Flow] with throttling applied based on the specified parameters.
 *
 * Example usage:
 *
 * ```
 *  flowOf(1, 2, 3, 4, 5)
 *      .throttle(strategy = ThrottleStrategy.Suspend) {
 *          // custom semaphore implementation
 *      }
 * ```
 */
fun <T> Flow<T>.throttle(
    strategy: ThrottleStrategy = ThrottleStrategy.Suspend,
    semaphore: suspend CoroutineScope.() -> AsyncSemaphore
): Flow<T> = ThrottleFlow(semaphore, strategy, this)
