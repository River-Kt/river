package com.river.core

import com.river.core.internal.UnfoldFlow
import kotlinx.coroutines.flow.Flow

/**
 * Creates a flow that unfolds its elements by successively applying the [unfolder] function.
 * The flow stops based on the [stopOnEmptyList] parameter or when the coroutine context is no longer active.
 *
 * @param stopOnEmptyList If true, the flow will stop when an empty list of elements is received. Defaults to false.
 *                        If false, the flow will continue unfolding elements indefinitely.
 * @param unfolder A lambda with receiver of type [ParallelismInfo] that produces a list of elements of type [T].
 *
 * @return A [Flow] of elements of type [T], unfolded using the provided [unfolder] function.
 *
 * Example usage:
 *
 * ```
 *  unfold(stopOnEmptyList = true) {
 *      listOf("Element 1", "Element 2", "Element 3")
 *  }.collect { println(it) }
 * ```
 */
fun <T> unfold(
    stopOnEmptyList: Boolean = false,
    unfolder: suspend ParallelismInfo.() -> List<T>
): Flow<T> =
    unfoldParallel(
        maxParallelism = 1,
        stopOnEmptyList = stopOnEmptyList,
        unfolder = unfolder
    )

/**
 * Creates a flow that unfolds its elements in parallel by successively applying the [unfolder] function.
 * The flow stops based on the [stopOnEmptyList] parameter or when the coroutine context is no longer active.
 * Parallelism is controlled by [maxParallelism], [minimumParallelism], and [increaseStrategy].
 *
 * @param maxParallelism The maximum number of parallel unfold operations. Defaults to 1.
 * @param stopOnEmptyList If true, the flow will stop when an empty list of elements is received. Defaults to false.
 *                        If false, the flow will continue unfolding elements indefinitely.
 * @param minimumParallelism The minimum number of parallel unfold operations. Defaults to 1.
 * @param increaseStrategy Determines how the parallelism increases when processing elements. Defaults to [ParallelismIncreaseStrategy.ByOne].
 * @param unfolder A lambda with receiver of type [ParallelismInfo] that produces a list of elements of type [T].
 *
 * @return A [Flow] of elements of type [T], unfolded in parallel using the provided [unfolder] function.
 *
 * Example usage:
 *
 * ```
 *  unfoldParallel(maxParallelism = 4, stopOnEmptyList = true) {
 *      listOf("Element 1", "Element 2", "Element 3")
 *  }.collect { println(it) }
 * ```
 */
fun <T> unfoldParallel(
    maxParallelism: Int,
    stopOnEmptyList: Boolean = false,
    minimumParallelism: Int = 1,
    increaseStrategy: ParallelismIncreaseStrategy = ParallelismIncreaseStrategy.ByOne,
    unfolder: suspend ParallelismInfo.() -> List<T>
): Flow<T> =
    UnfoldFlow(
        minimumParallelism = minimumParallelism,
        maxParallelism = maxParallelism,
        stopOnEmptyList = stopOnEmptyList,
        increaseStrategy = increaseStrategy,
        producer = unfolder
    )
