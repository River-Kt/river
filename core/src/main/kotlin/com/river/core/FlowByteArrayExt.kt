package com.river.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.nio.ByteBuffer
import java.nio.charset.Charset

/**
 * Converts the [Flow] of [ByteArray] to a [Flow] of [ByteBuffer].
 *
 * @return A new [Flow] of [ByteBuffer] converted from the original [Flow] of [ByteArray].
 */
fun Flow<ByteArray>.asByteBuffer(): Flow<ByteBuffer> = map { ByteBuffer.wrap(it) }

/**
 * Converts the [Flow] of [ByteArray] to a [Flow] of [String].
 *
 * @param charset The [Charset] to use for converting the bytes to strings. Defaults to the system's default charset.
 *
 * @return A new [Flow] of [String] converted from the original [Flow] of [ByteArray].
 */
fun Flow<ByteArray>.asString(charset: Charset = Charset.defaultCharset()): Flow<String> =
    map { String(it, charset) }
