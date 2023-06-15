package com.river.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.fold


/**
 * Sums the elements of this [Flow] of [Float] and returns the result.
 *
 * @return The sum of all elements emitted by the source Flow.
 *
 * Example usage:
 * ```
 * val flow = flowOf(1.0, 2.0, 3.0, 4.0, 5.0)
 * val sum = runBlocking { flow.sum() }
 * println(sum)  // prints: 15.0
 * ```
 */
suspend fun Flow<Float>.sum(): Double =
    fold(0.0) { acc, i -> acc + i }
