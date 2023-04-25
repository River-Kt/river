package com.river.core

import kotlin.time.Duration

sealed interface GroupStrategy {
    class Count(val size: Int) : GroupStrategy

    class TimeWindow(
        val size: Int,
        val duration: Duration
    ) : GroupStrategy
}
