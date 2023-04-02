package com.river.core

data class ParallelismInfo(
    val maxAllowedParallelism: Int,
    val currentParallelism: Int
)
