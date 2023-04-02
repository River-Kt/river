package com.river.core

import kotlinx.coroutines.flow.*
import java.nio.ByteBuffer
import java.nio.charset.Charset

/**
 * Converts the [Flow] of [String] to a [Flow] of [Byte] using the specified [charset].
 *
 * @param charset The [Charset] to use for converting the strings to bytes. Defaults to the system's default charset.
 *
 * @return A new [Flow] of [Byte] converted from the original [Flow] of [String].
 */
fun Flow<String>.asBytes(
    charset: Charset = Charset.defaultCharset()
) = asByteArray(charset)
    .map { it.toList() }
    .flatten()

/**
 * Converts the [Flow] of [String] to a [Flow] of [ByteArray] using the specified [charset].
 *
 * @param charset The [Charset] to use for converting the strings to byte arrays. Defaults to the system's default charset.
 *
 * @return A new [Flow] of [ByteArray] converted from the original [Flow] of [String].
 */
fun Flow<String>.asByteArray(
    charset: Charset = Charset.defaultCharset()
): Flow<ByteArray> = map { it.toByteArray(charset) }

/**
 * Converts the [Flow] of [String] to a [Flow] of [ByteBuffer] using the specified [charset].
 *
 * @param charset The [Charset] to use for converting the strings to byte buffers. Defaults to the system's default charset.
 *
 * @return A new [Flow] of [ByteBuffer] converted from the original [Flow] of [String].
 */
fun Flow<String>.asByteBuffer(
    charset: Charset = Charset.defaultCharset()
): Flow<ByteBuffer> = map { ByteBuffer.wrap(it.toByteArray(charset)) }

/**
 * Converts the [Flow] of [ByteArray] to a [Flow] of [ByteBuffer].
 *
 * @return A new [Flow] of [ByteBuffer] converted from the original [Flow] of [ByteArray].
 */
fun Flow<ByteArray>.asByteBuffer(): Flow<ByteBuffer> = map { ByteBuffer.wrap(it) }

/**
 * Converts the [Flow] of [ByteBuffer] to a [Flow] of [ByteArray].
 *
 * @return A new [Flow] of [ByteArray] converted from the original [Flow] of [ByteBuffer].
 */
fun Flow<ByteBuffer>.asByteArray(): Flow<ByteArray> =
    map { bb -> ByteArray(bb.remaining()).also { bb.get(it) } }

/**
 * Flattens a [Flow] of [List] of [ByteBuffer] and converts it to a [Flow] of [ByteArray].
 *
 * @return A new [Flow] of [ByteArray] converted and flattened from the original [Flow] of [List] of [ByteBuffer].
 */
fun Flow<List<ByteBuffer>>.flattenAsByteArray(): Flow<ByteArray> =
    flatten().asByteArray()
