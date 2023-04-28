package com.river.core

class ParallelismStrategy(
    val initial: ParallelismInfo,
    val increaseStrategy: (ParallelismInfo) -> ParallelismInfo = { it }
) {
    companion object {
        val disabled = static(1)

        fun static(parallelism: Int): ParallelismStrategy =
            ParallelismStrategy(ParallelismInfo(parallelism, parallelism))

        fun increaseBy(
            maximumParallelism: Int,
            minimumParallelism: Int = 1,
            factor: (Int) -> Int
        ): ParallelismStrategy =
            ParallelismStrategy(ParallelismInfo(maximumParallelism, minimumParallelism)) {
                val next = factor(it.currentParallelism)
                it.copy(currentParallelism = if (next > it.maxAllowedParallelism) it.maxAllowedParallelism else next)
            }

        fun increaseByOne(
            maximumParallelism: Int,
            minimumParallelism: Int = 1,
        ): ParallelismStrategy = increaseBy(maximumParallelism, minimumParallelism) { it + 1 }

        fun exponential(
            maximumParallelism: Int,
            minimumParallelism: Int = 1,
            factor: Int = 2
        ): ParallelismStrategy = increaseBy(maximumParallelism, minimumParallelism) { it * factor }

        fun maximumAllowedAfterFirstIteration(
            maximumParallelism: Int,
            minimumParallelism: Int = 1,
        ) = increaseBy(maximumParallelism, minimumParallelism) { maximumParallelism }
    }
}
