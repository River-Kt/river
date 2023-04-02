package com.river.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onCompletion

/**
 * Splits the [Flow] of [String] into lines, emitting each line as a separate element in the resulting [Flow].
 *
 * @return A new [Flow] where each element represents a line from the original [Flow] of [String].
 *
 * Example usage:
 *
 * ```
 *  val flow = flowOf(
 *                 "Hel",
 *                 "lo,",
 *                 " world!",
 *                 "\nW",
 *                 "elcome to River!"
 *             )
 *
 *  flow
 *      .lines()
 *      .collect(::println) //"Hello, world!", "Welcome to River!"
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
