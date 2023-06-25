package com.river.core

import com.river.core.internal.MapAsyncFlow
import com.river.core.internal.UnorderedMapAsyncFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect

/**
 * Applies the given transformation function [transform] to each value of the original [Flow]
 * and emits the results. The processing of items in the flow is concurrent and limited by [concurrency] level.
 *
 * The result flow emits the elements in an ordered manner.
 *
 * Example usage:
 *
 * ```
 * flowOf(1, 2, 3)
 *     .mapAsync(2) { it * 2 }
 *     .collect { println(it) } // Prints 2, 4, 6
 * ```
 *
 * @param concurrency The maximum number of concurrent transformations.
 * @param transform The transformation function to apply to each item.
 *
 * @return A new flow with the transformed items.
 */
fun <T, R> Flow<T>.mapAsync(
    concurrency: Int,
    transform: suspend (T) -> R
): Flow<R> = mapAsync(semaphore = { AsyncSemaphore(this, concurrency) }, transform = transform)

/**
 * Applies the given transformation function [transform] to each value of the original [Flow]
 * and emits the results. The concurrency is managed by an [AsyncSemaphore] created by
 * the [semaphore] suspending function.
 *
 * The result flow emits the elements in an ordered manner.
 *
 * Example usage:
 *
 * ```
 * val customSemaphore: suspend CoroutineScope.() -> AsyncSemaphore =  { AsyncSemaphore(this, 2) }
 *
 * flowOf(1, 2, 3)
 *     .mapAsync(customSemaphore) { it * 2 }
 *     .collect { println(it) } // Prints the results in an unordered manner
 * ```
 *
 * @param semaphore The suspending function that creates an [AsyncSemaphore].
 * @param transform The transformation function to apply to each item.
 *
 * @return A new flow with the transformed items.
 */
fun <T, R> Flow<T>.mapAsync(
    semaphore: suspend CoroutineScope.() -> AsyncSemaphore,
    transform: suspend (T) -> R
): Flow<R> = MapAsyncFlow(
    upstream = this,
    semaphore = semaphore,
    transform = transform
)

/**
 * Applies the given transformation function [transform] to each value of the original [Flow]
 * and emits the results. The processing of items in the flow is concurrent and limited by [concurrency] level.
 *
 * The result flow may emit the elements in an unordered manner, which makes this function faster than [mapAsync].
 *
 * Example usage:
 *
 * ```
 * flowOf(1, 2, 3)
 *     .unorderedMapAsync(2) { it * 2 }
 *     .collect { println(it) } // Prints 2, 4, 6
 * ```
 *
 * @param concurrency The maximum number of concurrent transformations.
 * @param transform The transformation function to apply to each item.
 *
 * @return A new flow with the transformed items.
 */
fun <T, R> Flow<T>.unorderedMapAsync(
    concurrency: Int,
    transform: suspend (T) -> R
): Flow<R> = unorderedMapAsync(semaphore = { AsyncSemaphore(this, concurrency) }, transform = transform)

/**
 * Applies the given transformation function [transform] to each value of the original [Flow]
 * and emits the results. The concurrency is managed by an [AsyncSemaphore] created by
 * the [semaphore] suspending function.
 *
 * The result flow may emit the elements in an unordered manner, which makes this function faster than [mapAsync].
 *
 * Example usage:
 *
 * ```
 * val customSemaphore: suspend CoroutineScope.() -> AsyncSemaphore =  { AsyncSemaphore(this, 2) }
 *
 * flowOf(1, 2, 3)
 *     .unorderedMapAsync(customSemaphore) { it * 2 }
 *     .collect { println(it) } // Prints the results in an unordered manner
 * ```
 *
 * @param semaphore The suspending function that creates an [AsyncSemaphore].
 * @param transform The transformation function to apply to each item.
 *
 * @return A new flow with the transformed items.
 */
fun <T, R> Flow<T>.unorderedMapAsync(
    semaphore: suspend CoroutineScope.() -> AsyncSemaphore,
    transform: suspend (T) -> R
): Flow<R> = UnorderedMapAsyncFlow(
    upstream = this,
    semaphore = semaphore,
    transform = transform
)

/**
 * Applies the given transformation function [transform] to each value of the original [Flow]
 * and emits the results. The processing of items in the flow is concurrent and limited by [concurrency] level.
 * Each transformed value is then flattened and emitted individually.
 *
 * The result flow emits the elements in an ordered manner.
 *
 * Example usage:
 *
 * ```
 * flowOf(1, 2, 3)
 *     .flatMapIterableAsync(2) { listOf(it, it + 1) }
 *     .collect { println(it) } // Prints 1, 2, 2, 3, 3, 4
 * ```
 *
 * @param concurrency The maximum number of concurrent transformations.
 * @param transform The transformation function to apply to each item.
 *
 * @return A new flow with the transformed and flattened items.
 */
fun <T, R> Flow<T>.flatMapIterableAsync(
    concurrency: Int,
    transform: suspend (T) -> Iterable<R>
): Flow<R> = mapAsync(concurrency, transform).flattenIterable()

/**
 * Applies the given transformation function [transform] to each value of the original [Flow]
 * and emits the results. The concurrency is managed by an [AsyncSemaphore] created by
 * the [semaphore] suspending function. Each transformed value is then flattened and emitted individually.
 *
 * The result flow emits the elements in an ordered manner.
 *
 * Example usage:
 *
 * ```
 * val customSemaphore: suspend CoroutineScope.() -> AsyncSemaphore =  { AsyncSemaphore(this, 2) }
 *
 * flowOf(1, 2, 3)
 *     .flatMapIterableAsync(customSemaphore) { listOf(it, it + 1) }
 *     .collect { println(it) } // Prints the results in an ordered manner
 * ```
 *
 * @param semaphore The suspending function that creates an [AsyncSemaphore].
 * @param transform The transformation function to apply to each item.
 *
 * @return A new flow with the transformed and flattened items.
 */
fun <T, R> Flow<T>.flatMapIterableAsync(
    semaphore: suspend CoroutineScope.() -> AsyncSemaphore,
    transform: suspend (T) -> Iterable<R>
): Flow<R> = mapAsync(semaphore, transform).flattenIterable()

/**
 * Applies the given transformation function [transform] to each value of the original [Flow]
 * and emits the results. The processing of items in the flow is concurrent and limited by [concurrency] level.
 * Each transformed value is then flattened and emitted individually.
 *
 * The result flow may emit the elements in an unordered manner, which makes this function faster than [flatMapIterableAsync].
 *
 * Example usage:
 *
 * ```
 * flowOf(1, 2, 3)
 *     .unorderedFlatMapIterableAsync(2) { listOf(it, it + 1) }
 *     .collect { println(it) } // Prints the results in an unordered manner
 * ```
 *
 * @param concurrency The maximum number of concurrent transformations.
 * @param transform The transformation function to apply to each item.
 *
 * @return A new flow with the transformed and flattened items.
 */
fun <T, R> Flow<T>.unorderedFlatMapIterableAsync(
    concurrency: Int,
    transform: suspend (T) -> Iterable<R>
): Flow<R> = unorderedMapAsync(concurrency, transform).flattenIterable()

/**
 * Applies the given transformation function [transform] to each value of the original [Flow]
 * and emits the results. The concurrency is managed by an [AsyncSemaphore] created by
 * the [semaphore] suspending function. Each transformed value is then flattened and emitted individually.
 *
 * The result flow may emit the elements in an unordered manner, which makes this function faster than [flatMapIterableAsync].
 *
 * Example usage:
 *
 * ```
 * val customSemaphore: suspend CoroutineScope.() -> AsyncSemaphore =  { AsyncSemaphore(this, 2) }
 *
 * flowOf(1, 2, 3)
 *     .unorderedFlatMapIterableAsync(customSemaphore) { listOf(it, it + 1) }
 *     .collect { println(it) } // Prints the results in an unordered manner
 * ```
 *
 * @param semaphore The suspending function that creates an [AsyncSemaphore].
 * @param transform The transformation function to apply to each item.
 *
 * @return A new flow with the transformed and flattened items.
 */
fun <T, R> Flow<T>.unorderedFlatMapIterableAsync(
    semaphore: suspend CoroutineScope.() -> AsyncSemaphore,
    transform: suspend (T) -> Iterable<R>
): Flow<R> = unorderedMapAsync(semaphore, transform).flattenIterable()

/**
 * Applies the given function [block] to each value of the original [Flow] and
 * reemits them downstream. The processing of items in the flow is concurrent and limited by [concurrency] level.
 *
 * The result flow emits the elements in an ordered manner.
 *
 * Example usage:
 *
 * ```
 * flowOf(1, 2, 3)
 *     .onEachAsync(2) { println("Processing $it") }
 *     .collect() // Prints "Processing 1", "Processing 2", "Processing 3"
 * ```
 *
 * @param concurrency The maximum number of concurrent transformations.
 * @param block The function to apply to each item.
 *
 * @return The same flow with the side-effecting [block] applied to each item.
 */
fun <T> Flow<T>.onEachAsync(
    concurrency: Int,
    block: suspend (T) -> Unit
): Flow<T> = mapAsync(concurrency) { it.also { block(it) } }

/**
 * Applies the given function [block] to each value of the original [Flow] and
 * reemits them downstream. The concurrency is managed by an [AsyncSemaphore] created by
 * the [semaphore] suspending function.
 *
 * The result flow emits the elements in an ordered manner.
 *
 * Example usage:
 *
 * ```
 * val customSemaphore: suspend CoroutineScope.() -> AsyncSemaphore =  { AsyncSemaphore(this, 2) }
 *
 * flowOf(1, 2, 3)
 *     .onEachAsync(customSemaphore) { println("Processing $it") }
 *     .collect() // Prints "Processing 1", "Processing 2", "Processing 3" in an ordered manner
 * ```
 *
 * @param semaphore The suspending function that creates an [AsyncSemaphore].
 * @param block The function to apply to each item.
 *
 * @return The same flow with the side-effecting [block] applied to each item.
 */
fun <T> Flow<T>.onEachAsync(
    semaphore: suspend CoroutineScope.() -> AsyncSemaphore,
    block: suspend (T) -> Unit
): Flow<T> = mapAsync(semaphore) { it.also { block(it) } }

/**
 * Applies the given function [block] to each value of the original [Flow] and
 * reemits them downstream. The processing of items in the flow is concurrent and limited by [concurrency] level.
 *
 * The result flow may emit the elements in an unordered manner, which makes this function faster than [onEachAsync].
 *
 * Example usage:
 *
 * ```
 * flowOf(1, 2, 3)
 *     .unorderedOnEachAsync(2) { println("Processing $it") }
 *     .collect() // Prints "Processing 1", "Processing 2", "Processing 3" in an unordered manner
 * ```
 *
 * @param concurrency The maximum number of concurrent transformations.
 * @param block The function to apply to each item.
 *
 * @return A new flow with the same elements, side-effecting [block] applied to each item, possibly unordered.
 */
fun <T> Flow<T>.unorderedOnEachAsync(
    concurrency: Int,
    block: suspend (T) -> Unit
): Flow<T> = unorderedMapAsync(concurrency) { it.also { block(it) } }

/**
 * Applies the given function [block] to each value of the original [Flow] and
 * reemits them downstream. The concurrency is managed by an [AsyncSemaphore] created by
 * the [semaphore] suspending function.
 *
 * The result flow may emit the elements in an unordered manner, which makes this function faster than [onEachAsync].
 *
 * Example usage:
 *
 * ```
 * val customSemaphore: suspend CoroutineScope.() -> AsyncSemaphore =  { AsyncSemaphore(this, 2) }
 *
 * flowOf(1, 2, 3)
 *     .unorderedOnEachAsync(customSemaphore) { println("Processing $it") }
 *     .collect() // Prints "Processing 1", "Processing 2", "Processing 3" in an unordered manner
 * ```
 *
 * @param semaphore The suspending function that creates an [AsyncSemaphore].
 * @param block The function to apply to each item.
 *
 * @return A new flow with the same elements, side-effecting [block] applied to each item, possibly unordered.
 */
fun <T> Flow<T>.unorderedOnEachAsync(
    semaphore: suspend CoroutineScope.() -> AsyncSemaphore,
    block: suspend (T) -> Unit
): Flow<T> = unorderedMapAsync(semaphore) { it.also { block(it) } }

/**
 * Collects all the values emitted by the [Flow], applying the [block] function to each value.
 * The processing of items in the flow is concurrent and limited by [concurrency] level.
 *
 * Example usage:
 *
 * ```
 * flowOf(1, 2, 3)
 *     .collectAsync(2) { println("Processing $it") }
 * ```
 *
 * @param concurrency The maximum number of concurrent transformations.
 * @param block The function to apply to each item.
 */
suspend fun <T> Flow<T>.collectAsync(
    concurrency: Int,
    block: suspend (T) -> Unit
): Unit = onEachAsync(concurrency, block).collect()

/**
 * Collects all the values emitted by the [Flow], applying the [block] function to each value.
 * The concurrency is managed by an [AsyncSemaphore] created by the [semaphore] suspending function.
 *
 * Example usage:
 *
 * ```
 * val customSemaphore: suspend CoroutineScope.() -> AsyncSemaphore =  { AsyncSemaphore(this, 2) }
 *
 * flowOf(1, 2, 3)
 *     .collectAsync(customSemaphore) { println("Processing $it") }
 * ```
 *
 * @param semaphore The suspending function that creates an [AsyncSemaphore].
 * @param block The transformation function to apply to each item.
 */
suspend fun <T> Flow<T>.collectAsync(
    semaphore: suspend CoroutineScope.() -> AsyncSemaphore,
    block: suspend (T) -> Unit
): Unit = onEachAsync(semaphore, block).collect()
