package com.river.core

import kotlinx.coroutines.flow.*
import java.nio.charset.Charset

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
 * @param charset The [Charset] to use for converting the bytes to strings. Defaults to the system's default charset.
 *
 * @return A new [Flow] of [String] converted from the original [Flow] of [Byte].
 */
fun Flow<Byte>.asString(charset: Charset = Charset.defaultCharset()): Flow<String> =
    map { String(listOf(it).toByteArray(), charset) }

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
