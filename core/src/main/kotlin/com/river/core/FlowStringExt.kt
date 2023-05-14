package com.river.core

import kotlinx.coroutines.flow.*
import java.nio.ByteBuffer
import java.nio.charset.Charset

/**
 * Splits and buffers the [Flow] of [String] into lines, emitting each line as a separate element in the resulting [Flow].
 *
 * This function buffers incoming strings until it encounters a line break (`\n`). It then emits each line as a separate string.
 * If the incoming strings do not contain any line breaks, they are buffered until a line break is encountered or the flow is completed.
 *
 * @return A new [Flow] where each element represents a line from the original [Flow] of [String].
 *
 * Example usage:
 *
 * ```
 * val flow = flowOf(
 *    "Hel",
 *    "lo,",
 *    " world!",
 *    "\nW",
 *    "elcome to River!"
 * )
 *
 * flow
 *     .lines()
 *     .collect(::println) //"Hello, world!", "Welcome to River!"
 * ```
 */
fun Flow<String>.lines() = flow {
    var buffer = ""

    onCompletion { emit("\n") }
        .collect {
            buffer += it

            val stopsWithLineBreak = buffer.endsWith("\n")
            val lines = buffer.split("\n")

            if (stopsWithLineBreak) {
                lines.filterNot { it.isBlank() }.forEach { emit(it) }
                buffer = ""
            } else if (lines.size > 1) {
                lines.dropLast(1).forEach { emit(it) }
                buffer = lines.last()
            }
        }
}


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
