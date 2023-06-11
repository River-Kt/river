package com.river.core

import com.river.core.internal.StopException
import com.river.core.internal.StoppableFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector

/**
 * Creates a [Flow] with the ability to stop its collection early based on a custom condition.
 *
 * @param block A suspending lambda expression that takes a [StoppableFlowCollector] and defines the logic
 *              for collecting and emitting elements. To stop the collection early, call the [halt] method
 *              on the [StoppableFlowCollector] with a message describing the reason for stopping.
 *
 * @return A new [Flow] with the ability to stop its collection early based on a custom condition.
 *
 * Example usage:
 *
 * ```
 *  stoppableFlow<Int> {
 *      for (i in 1..10) {
 *          if (i == 5) halt("Stopping at 5")
 *          emit(i)
 *      }
 *  } // 1, 2, 3, 4
 * ```
 */
fun <T> stoppableFlow(block: suspend StoppableFlowCollector<T>.() -> Unit): Flow<T> =
    StoppableFlow { block(StoppableFlowCollector(this)) }

/**
 * Completes the [Flow] early if the specified [stopPredicate] is met for any element.
 *
 * @param stopPredicate A suspend function that takes an element of type [T] and returns a [Boolean].
 *                      If the function returns `true`, the flow will complete early and stop emitting elements.
 *
 * @return A new [Flow] that completes early if the [stopPredicate] is met for any element.
 *
 * Example usage:
 *
 * ```
 *  flowOf(1, 2, 3, 4, 5)
 *      .earlyCompleteIf { it == 3 }
 *      .collect(::println) //1, 2, 3
 * ```
 */
fun <T> Flow<T>.earlyCompleteIf(
    stopPredicate: suspend (T) -> Boolean
): Flow<T> =
    stoppableFlow {
        collect {
            val matches = stopPredicate(it)
            if (matches) halt("got a false predicate, completing the flow")
            else emit(it)
        }
    }

