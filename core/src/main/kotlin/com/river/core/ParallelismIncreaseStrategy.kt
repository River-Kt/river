package com.river.core

fun interface ParallelismIncreaseStrategy {
    operator fun invoke(info: ParallelismInfo): ParallelismInfo

    companion object {
        fun by(f: (ParallelismInfo) -> Int): ParallelismIncreaseStrategy = ParallelismIncreaseStrategy {
            val next = f(it)
            it.copy(currentParallelism = if (next > it.maxAllowedParallelism) it.maxAllowedParallelism else next)
        }

        val ByOne = by { it.currentParallelism + 1 }
        val Exponential = by { it.currentParallelism * 2 }
        val MaxAllowedAfterReceive = by { it.maxAllowedParallelism }
    }
}
