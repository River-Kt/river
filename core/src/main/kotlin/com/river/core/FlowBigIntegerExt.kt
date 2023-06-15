package com.river.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.fold
import java.math.BigInteger


/**
 * Sums the elements of this [Flow] of [BigInteger] and returns the result.
 *
 * @return The sum of all elements emitted by the source Flow.
 *
 * Example usage:
 * ```
 * val flow = flowOf(1L, 2L, 3L, 4L, 5L).map(::BigInteger)
 * val sum = runBlocking { flow.sum() }
 * println(sum)  // prints: 15
 * ```
 */
suspend fun Flow<BigInteger>.sum(): BigInteger =
    fold(BigInteger.ZERO) { acc, i -> acc + i }
