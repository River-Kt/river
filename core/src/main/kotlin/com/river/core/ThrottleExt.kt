package com.river.core

import com.river.core.internal.ThrottleFlow
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
): Flow<T> = ThrottleFlow(elementsPerInterval, interval, strategy, this)
