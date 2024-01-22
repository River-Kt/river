package com.river.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.fold

/**
 * Sums the elements of this [Flow] of [Short] and returns the result.
 *
 * @return The sum of all elements emitted by the source Flow.
 *
 * Example usage:
 * ```
 * val flow = flowOf(1, 2, 3, 4, 5).map { it.toShort() }
 * val sum = runBlocking { flow.sum() }
 * println(sum)  // prints: 15
 * ```
 */
suspend fun Flow<Short>.sum(): Long =
    fold(0L) { acc, i -> acc + i }
