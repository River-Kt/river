package io.river.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion

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
