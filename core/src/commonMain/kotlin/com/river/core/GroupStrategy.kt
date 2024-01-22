package com.river.core

import kotlin.time.Duration

/**
 * A sealed interface representing different strategies for grouping items in a Flow.
 * The GroupStrategy can be used in various contexts to control the way items are
 * grouped together, optimizing the trade-off between latency and throughput.
 *
 * Implementations of this interface can be used to specify the grouping behavior for items in a Flow,
 * such as grouping by count or by time window.
 *
 * Example usage:
 * ```
 * val groupedFlow: Flow<List<T>> = sourceFlow.chunked(GroupStrategy.Count(10))
 * ...
 * val splitFlow: Flow<Flow<T>> = sourceFlow.split(GroupStrategy.Count(10))
 * ```
 *
 * Subclasses:
 * - [Count]: Groups items based on a fixed count.
 * - [TimeWindow]: Groups items based on a time window with a maximum item count.
 */
sealed interface GroupStrategy {
    /**
     * A class representing a count-based grouping strategy.
     *
     * @property size The number of items to group together.
     */
    class Count(val size: Int) : GroupStrategy

    /**
     * A class representing a time window-based grouping strategy.
     *
     * @property size The maximum number of items to group together.
     * @property duration The time window duration for grouping items.
     */
    class TimeWindow(
        val size: Int,
        val duration: Duration
    ) : GroupStrategy
}
