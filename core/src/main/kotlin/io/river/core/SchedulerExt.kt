package io.river.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration

fun CoroutineScope.tick(
    intervalDuration: Duration,
    f: suspend () -> Unit
): Job =
    launch {
        delay(intervalDuration)
        f()
        tick(intervalDuration, f)
    }
