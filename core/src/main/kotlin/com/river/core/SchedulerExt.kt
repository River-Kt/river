package com.river.core

import kotlinx.coroutines.*
import kotlin.time.Duration

@ExperimentalRiverApi
fun CoroutineScope.tick(
    intervalDuration: Duration,
    f: suspend () -> Unit
): Job =
    launch {
        while (isActive) {
            delay(intervalDuration)
            f()
        }
    }
