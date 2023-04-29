package com.river.core

import com.river.core.internal.PollingFlow
import kotlinx.coroutines.flow.*

/**
 * Creates a flow that continuously polls elements in parallel by successively applying the [f] function.
 * The flow stops based on the [stopOnEmptyList] parameter or when the coroutine context is no longer active.
 * Parallelism is controlled by the [parallelism] strategy.
 *
 * @param parallelism The [ParallelismStrategy] to control the number of parallel polling operations allowed. Defaults to a static strategy with parallelism of 1.
 * @param stopOnEmptyList If true, the flow will stop when an empty list of elements is received. Defaults to false.
 *                        If false, the flow will continue polling elements indefinitely.
 * @param f A lambda with receiver of type [ParallelismInfo] that produces a list of elements of type [T].
 *
 * @return A [Flow] of elements of type [T], polled in parallel using the provided [f] function.
 *
 * Example usage:
 *
 * ```
 *  suspend fun fetchData(): List<Data> = ... // fetch data from somewhere
 *
 *  poll(ParallelismStrategy.increaseByOne(4), stopOnEmptyList = true) { fetchData() }
 *      .collect { println(it) }
 * ```
 */
fun <T> poll(
    parallelism: ParallelismStrategy = ParallelismStrategy.disabled,
    stopOnEmptyList: Boolean = false,
    f: suspend ParallelismInfo.() -> List<T>
): Flow<T> =
    PollingFlow(
        stopOnEmptyList = stopOnEmptyList,
        parallelism = parallelism,
        producer = f
    )

/**
 * Creates a flow that continuously polls elements using the [f] function, starting with an initial state [initial].
 *
 * The flow stops when the [shouldStop] function returns true for the current state or when the coroutine context is no longer active.
 *
 * The primary difference between this function and the regular [poll] function is that [pollWithState]
 * maintains state from the previous polling iteration. This can be particularly useful in situations
 * where some level of state control is required, such as HTTP pagination, stream polling, and more.
 *
 * @param initial The initial state of type [S] to begin the polling process.
 * @param shouldStop A predicate function to determine if the polling should stop based on the current state. Defaults to a function that always returns false.
 * @param f A suspending function that takes the current state [S] as input and returns a pair consisting of the next state [S] and a list of elements of type [T].
 *
 * @return A [Flow] of elements of type [T], polled by the provided [f] function.
 *
 * Example usage:
 *
 * ```
 * data class ApiResponse<T>(
 *     val items: List<T>,
 *     val nextPage: Int?,
 *     val totalItems: Int
 * )
 *
 * suspend fun fetchPage(page: Int): ApiResponse<String> = // Fetch data from some API
 *
 * pollWithState(
 *     initial = 1, // Initial page number
 *     shouldStop = { it == -1 } // Stop when there are no more pages to fetch
 * ) { currentPage ->
 *     val response = fetchPage(currentPage)
 *     val nextState = response.nextPage
 *     val items = response.items
 *
 *     (nextState ?: -1) to items
 * }.collect { item -> println(item) }
 * ```
 */
fun <T, S> pollWithState(
    initial: S,
    shouldStop: (S) -> Boolean = { false },
    f: suspend (S) -> Pair<S, List<T>>
): Flow<T> = flow {
    var last: S = initial

    poll { listOf(f(last).also { last = it.first }) }
        .transformWhile { (state, items) ->
            emit(items)
            !shouldStop(state)
        }
        .flatten()
        .also { emitAll(it) }
}
