package com.river.core

import kotlin.time.Duration

sealed interface ChunkStrategy {
    class Count(val size: Int) : ChunkStrategy

    class TimeWindow(
        val size: Int,
        val duration: Duration
    ) : ChunkStrategy
}
