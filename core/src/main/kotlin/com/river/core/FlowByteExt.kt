package com.river.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.flow.map

/**
 * Converts the [Flow] of [Byte] to a [Flow] of [ByteArray].
 *
 * @return A new [Flow] of [ByteArray] converted from the original [Flow] of [Byte].
 */
fun Flow<Byte>.asByteArray(
    groupStrategy: GroupStrategy = GroupStrategy.Count(8)
): Flow<ByteArray> =
    chunked(groupStrategy)
        .map { it.toByteArray() }

/**
 * Converts the [Flow] of [Byte] to a [Flow] of [String].
 *
 * @return A new [Flow] of [String] converted from the original [Flow] of [Byte].
 */
fun Flow<Byte>.asString(
    groupStrategy: GroupStrategy = GroupStrategy.Count(8)
): Flow<String> =
    asByteArray(groupStrategy)
        .asString()

/**
 * Sums the elements of this [Flow] of [Byte] and returns the result.
 *
 * @return The sum of all elements emitted by the source Flow.
 *
 * Example usage:
 * ```
 * val flow = flowOf(1, 2, 3, 4, 5).map { it.toByte() }
 * val sum = runBlocking { flow.sum() }
 * println(sum)  // prints: 15
 * ```
 */
suspend fun Flow<Byte>.sum(): Long =
    fold(0L) { acc, i -> acc + i }
