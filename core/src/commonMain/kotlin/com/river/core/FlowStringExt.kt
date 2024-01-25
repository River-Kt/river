package com.river.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion

/**
 * Splits and buffers the [Flow] of [String] based on the provided delimiter, emitting each piece as a separate element in the resulting [Flow].
 *
 * This function buffers incoming strings until it encounters the provided delimiter. It then emits each piece as a separate string.
 * If the incoming strings do not contain any delimiter, they are buffered until the delimiter is encountered or the flow is completed.
 *
 * @return A new [Flow] where each element represents a piece split by a delimiter from the original [Flow] of [String].
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
 *     .splitEvery("\n")
 *     .collect(::println) //"Hello, world!", "Welcome to River!"
 * ```
 */
fun Flow<String>.splitEvery(
    delimiter: String
) = flow {
    var buffer = ""

    onCompletion { emit(delimiter) }
        .collect { chunk ->
            buffer += chunk

            val stopsWithLineBreak = buffer.endsWith(delimiter)
            val pieces = buffer.split(delimiter)

            if (stopsWithLineBreak) {
                pieces.filterNot { it.isBlank() }.forEach { emit(it) }
                buffer = ""
            } else if (pieces.size > 1) {
                pieces.dropLast(1).forEach { emit(it) }
                buffer = pieces.last()
            }
        }
}

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
fun Flow<String>.lines() = splitEvery("\n")
