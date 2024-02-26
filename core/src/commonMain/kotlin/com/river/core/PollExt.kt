package com.river.core

import com.river.core.internal.PollingFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.transformWhile
import kotlin.time.Duration

/**
 * Creates a flow that continuously polls elements concurrently by successively applying the [f] function.
 * The flow stops based on the [stopOnEmptyList] parameter or when the coroutine context is no longer active.
 * Concurrency is controlled by the [concurrency] strategy.
 *
 * @param concurrency The [ConcurrencyStrategy] to control the number of concurrent polling operations allowed. Defaults to a static strategy with concurrency of 1.
 * @param stopOnEmptyList If true, the flow will stop when an empty list of elements is received. Defaults to false.
 *                        If false, the flow will continue polling elements indefinitely.
 * @param f A lambda with receiver of type [ConcurrencyInfo] that produces a list of elements of type [T].
 *
 * @return A [Flow] of elements of type [T], polled concurrently using the provided [f] function.
 *
 * Example usage:
 *
 * ```
 *  suspend fun fetchData(): List<Data> = ... // fetch data from somewhere
 *
 *  poll(ConcurrencyStrategy.increaseByOne(4), stopOnEmptyList = true) { fetchData() }
 *      .collect { println(it) }
 * ```
 */
fun <T> poll(
    concurrency: ConcurrencyStrategy = ConcurrencyStrategy.disabled,
    stopOnEmptyList: Boolean = false,
    interval: Duration? = null,
    f: suspend ConcurrencyInfo.() -> List<T>
): Flow<T> =
    PollingFlow(
        stopOnEmptyList = stopOnEmptyList,
        concurrency = concurrency,
        interval = interval,
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
 *     val nextPage: Int?
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
@ExperimentalRiverApi
fun <T, S> pollWithState(
    initial: S,
    interval: Duration? = null,
    shouldStop: (S) -> Boolean = { false },
    f: suspend (S) -> Pair<S, List<T>>
): Flow<T> = flow {
    var last: S = initial

    poll(interval = interval) { listOf(f(last).also { last = it.first }) }
        .transformWhile { (state, items) ->
            emit(items)
            !shouldStop(state)
        }
        .flattenIterable()
        .also { emitAll(it) }
}
