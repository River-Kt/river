package com.river.core

/**
 * A class representing a parallelism strategy for handling concurrent operations.
 *
 * @property initial The initial [ParallelismInfo] containing information about maximum allowed and current parallelism levels.
 * @property increaseStrategy A function to update the [ParallelismInfo] based on the provided strategy.
 *
 * The companion object provides several pre-defined strategies:
 * - [disabled]: A strategy with no parallelism (tasks are executed sequentially).
 * - [fixed]: A strategy with a fixed level of parallelism.
 * - [increaseBy]: A strategy that increases parallelism based on a custom factor function.
 * - [increaseByOne]: A strategy that increases parallelism by one.
 * - [exponential]: A strategy that increases parallelism exponentially.
 * - [maximumAllowedAfterFirstIteration]: A strategy that uses the maximum parallelism level allowed after the first iteration.
 */
class ParallelismStrategy(
    val initial: ParallelismInfo,
    val increaseStrategy: (ParallelismInfo) -> ParallelismInfo = { it }
) {
    companion object {
        /**
         * A disabled parallelism strategy with no parallelism (tasks are executed sequentially).
         */
        val disabled = fixed(1)

        /**
         * Creates a static parallelism strategy with a fixed level of parallelism.
         *
         * @param parallelism The fixed level of parallelism.
         *
         * @return A [ParallelismStrategy] with the specified static parallelism level.
         */
        fun fixed(parallelism: Int): ParallelismStrategy =
            ParallelismStrategy(ParallelismInfo(parallelism, parallelism))

        /**
         * Creates a custom parallelism strategy that increases parallelism based on the provided factor function.
         *
         * @param maximumParallelism The maximum parallelism level allowed.
         * @param minimumParallelism The minimum parallelism level allowed (defaults to 1).
         * @param factor A function that calculates the next parallelism level based on the current level.
         *
         * @return A [ParallelismStrategy] with the specified custom increase strategy.
         */
        fun increaseBy(
            maximumParallelism: Int,
            minimumParallelism: Int = 1,
            factor: (Int) -> Int
        ): ParallelismStrategy =
            ParallelismStrategy(ParallelismInfo(maximumParallelism, minimumParallelism)) {
                val next = factor(it.currentParallelism)
                it.copy(currentParallelism = if (next > it.maxAllowedParallelism) it.maxAllowedParallelism else next)
            }

        /**
         * Creates a parallelism strategy that increases parallelism by one for each step.
         *
         * @param maximumParallelism The maximum parallelism level allowed.
         * @param minimumParallelism The minimum parallelism level allowed (defaults to 1).
         *
         * @return A [ParallelismStrategy] with an increase-by-one strategy.
         */
        fun increaseByOne(
            maximumParallelism: Int,
            minimumParallelism: Int = 1,
        ): ParallelismStrategy = increaseBy(maximumParallelism, minimumParallelism) { it + 1 }

        /**
         * Creates an exponential parallelism strategy that increases parallelism exponentially for each step.
         *
         * @param maximumParallelism The maximum parallelism level allowed.
         * @param minimumParallelism The minimum parallelism level allowed (defaults to 1).
         * @param factor The exponential factor (defaults to 2).
         *
         * @return A [ParallelismStrategy] with an exponential increase strategy.
         */
        fun exponential(
            maximumParallelism: Int,
            minimumParallelism: Int = 1,
            factor: Int = 2
        ): ParallelismStrategy = increaseBy(maximumParallelism, minimumParallelism) { it * factor }

        /**
         * Creates a parallelism strategy that uses the maximum parallelism level allowed after the first iteration.
         *
         * @param maximumParallelism The maximum parallelism level allowed.
         * @param minimumParallelism The minimum parallelism level allowed (defaults to 1).
         *
         * @return A [ParallelismStrategy] that switches to the maximum allowed parallelism after the first iteration.
         */
        fun maximumAllowedAfterFirstIteration(
            maximumParallelism: Int,
            minimumParallelism: Int = 1,
        ) = increaseBy(maximumParallelism, minimumParallelism) { maximumParallelism }
    }
}
