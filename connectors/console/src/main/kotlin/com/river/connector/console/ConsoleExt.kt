@file:OptIn(ExperimentalCoroutinesApi::class)

package com.river.connector.console

import com.river.core.poll
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.invoke

enum class Print { BREAK_LINE, NO_BREAK }
enum class OutType { DEFAULT, ERROR }

/**
 * Creates a flow of strings read from the console input (stdin) using the specified dispatcher.
 *
 * @param dispatcher The CoroutineDispatcher to use for reading from stdin. Defaults to a single-threaded dispatcher.
 *
 * @return A flow of strings read from the console input.
 *
 * Example usage:
 *
 * ```
 *  val consoleInputFlow = consoleIn()
 *  consoleInputFlow.collect { input -> println("You entered: $input") }
 * ```
 */
fun consoleIn(
    dispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(1)
): Flow<String> =
    poll { dispatcher { listOf(String(System.`in`.readBytes())) } }

/**
 * Outputs the flow of objects to the console (stdout or stderr) using the specified dispatcher, print, and mapper.
 *
 * @param dispatcher The CoroutineDispatcher to use for writing to the console. Defaults to a single-threaded dispatcher.
 * @param print The print mode (Print.BREAK_LINE or Print.NO_BREAK) to use for console output.
 * @param mapper A function that maps each object in the flow to a pair of (output item, output type).
 *               Defaults to the original item and OutType.DEFAULT.
 *
 * @return The original flow of objects.
 *
 * Example usage:
 *
 * ```
 *  data class Person(val name: String, val age: Int)
 *  val peopleFlow = flowOf(Person("Alice", 30), Person("Bob", 25))
 *
 *  peopleFlow.consoleOut().collect()
 * ```
 */
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
