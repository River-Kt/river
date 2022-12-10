@file:OptIn(ExperimentalCoroutinesApi::class)

package io.river.connector.console

import io.river.core.unfold
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.invoke

enum class Print { BREAK_LINE, NO_BREAK }
enum class OutType { DEFAULT, ERROR }

fun consoleIn(
    dispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(1)
): Flow<String> =
    unfold { dispatcher { listOf(String(System.`in`.readBytes())) } }

fun <T : Any> Flow<T>.consoleOut(
    dispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(1),
    print: Print = Print.BREAK_LINE,
    mapper: suspend (T) -> Pair<Any, OutType> = { it to OutType.DEFAULT },
) = map(mapper)
    .onEach { (item, out) ->
        val outType = when (out) {
            OutType.DEFAULT -> System.out
            OutType.ERROR -> System.err
        }

        dispatcher {
            when (print) {
                Print.BREAK_LINE -> outType.println(item)
                Print.NO_BREAK -> outType.print(item)
            }
        }
    }
