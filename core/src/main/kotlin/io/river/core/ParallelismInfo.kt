package io.river.core

data class ParallelismInfo(
    val maxAllowedParallelism: Int,
    val currentParallelism: Int
)
