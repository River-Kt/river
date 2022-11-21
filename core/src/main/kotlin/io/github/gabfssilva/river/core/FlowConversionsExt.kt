package io.github.gabfssilva.river.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.nio.ByteBuffer
import java.nio.charset.Charset

fun Flow<String>.asByteArray(
    charset: Charset = Charset.defaultCharset()
): Flow<ByteArray> = map { it.toByteArray(charset) }

fun Flow<String>.asByteBuffer(
    charset: Charset = Charset.defaultCharset()
): Flow<ByteBuffer> = map { ByteBuffer.wrap(it.toByteArray(charset)) }

fun Flow<ByteArray>.asByteBuffer(): Flow<ByteBuffer> = map { ByteBuffer.wrap(it) }

fun Flow<ByteBuffer>.asByteArray(): Flow<ByteArray> =
    map { bb -> ByteArray(bb.remaining()).also { bb.get(it) } }

fun Flow<List<ByteBuffer>>.flattenAsByteArray(): Flow<ByteArray> =
    flatten().asByteArray()