package com.river.core

/**
 * A class representing a concurrency strategy for handling concurrent operations.
 *
 * @property initial The initial [ConcurrencyInfo] containing information about maximum allowed and current concurrency levels.
 * @property increaseStrategy A function to update the [ConcurrencyInfo] based on the provided strategy.
 *
 * The companion object provides several pre-defined strategies:
 * - [disabled]: A strategy with no concurrency (tasks are executed sequentially).
 * - [fixed]: A strategy with a fixed level of concurrency.
 * - [increaseBy]: A strategy that increases concurrency based on a custom factor function.
 * - [increaseByOne]: A strategy that increases concurrency by one.
 * - [exponential]: A strategy that increases concurrency exponentially.
 * - [maximumAllowedAfterFirstIteration]: A strategy that uses the maximum concurrency level allowed after the first iteration.
 */
class ConcurrencyStrategy(
    val initial: ConcurrencyInfo,
    val increaseStrategy: (ConcurrencyInfo) -> ConcurrencyInfo = { it }
) {
    companion object {
        /**
         * A disabled concurrency strategy with no concurrency (tasks are executed sequentially).
         */
        val disabled = fixed(1)

        /**
         * Creates a static concurrency strategy with a fixed level of concurrency.
         *
         * @param concurrency The fixed level of concurrency.
         *
         * @return A [ConcurrencyStrategy] with the specified static concurrency level.
         */
        fun fixed(concurrency: Int): ConcurrencyStrategy =
            ConcurrencyStrategy(ConcurrencyInfo(concurrency, concurrency))

        /**
         * Creates a custom concurrency strategy that increases concurrency based on the provided factor function.
         *
         * @param maximumConcurrency The maximum concurrency level allowed.
         * @param minimumConcurrency The minimum concurrency level allowed (defaults to 1).
         * @param factor A function that calculates the next concurrency level based on the current level.
         *
         * @return A [ConcurrencyStrategy] with the specified custom increase strategy.
         */
        fun increaseBy(
            maximumConcurrency: Int,
            minimumConcurrency: Int = 1,
            factor: (Int) -> Int
        ): ConcurrencyStrategy =
            ConcurrencyStrategy(ConcurrencyInfo(maximumConcurrency, minimumConcurrency)) {
                val next = factor(it.current)
                it.copy(current = if (next > it.maximum) it.maximum else next)
            }

        /**
         * Creates a concurrency strategy that increases concurrency by one for each step.
         *
         * @param maximumConcurrency The maximum concurrency level allowed.
         * @param minimumConcurrency The minimum concurrency level allowed (defaults to 1).
         *
         * @return A [ConcurrencyStrategy] with an increase-by-one strategy.
         */
        fun increaseByOne(
            maximumConcurrency: Int,
            minimumConcurrency: Int = 1,
        ): ConcurrencyStrategy = increaseBy(maximumConcurrency, minimumConcurrency) { it + 1 }

        /**
         * Creates an exponential concurrency strategy that increases concurrency exponentially for each step.
         *
         * @param maximumConcurrency The maximum concurrency level allowed.
         * @param minimumconcurrency The minimum concurrency level allowed (defaults to 1).
         * @param factor The exponential factor (defaults to 2).
         *
         * @return A [ConcurrencyStrategy] with an exponential increase strategy.
         */
        fun exponential(
            maximumConcurrency: Int,
            minimumConcurrency: Int = 1,
            factor: Int = 2
        ): ConcurrencyStrategy = increaseBy(maximumConcurrency, minimumConcurrency) { it * factor }

        /**
         * Creates a concurrency strategy that uses the maximum concurrency level allowed after the first iteration.
         *
         * @param maximumConcurrency The maximum concurrency level allowed.
         * @param minimumConcurrency The minimum concurrency level allowed (defaults to 1).
         *
         * @return A [ConcurrencyStrategy] that switches to the maximum allowed concurrency after the first iteration.
         */
        fun maximumAllowedAfterFirstIteration(
            maximumConcurrency: Int,
            minimumConcurrency: Int = 1,
        ) = increaseBy(maximumConcurrency, minimumConcurrency) { maximumConcurrency }
    }
}
