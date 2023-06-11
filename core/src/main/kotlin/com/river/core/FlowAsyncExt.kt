package com.river.core

import com.river.core.internal.MapAsyncFlow
import com.river.core.internal.UnorderedMapAsyncFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map

/**
 * The [mapAsync] function is similar to the [map] function
 * since it transforms each element via the [transform] function.
 *
 * It works, however, in a parallel way, which means that multiple elements can be
 * processed at the same time, especially useful for more intensive tasks.
 *
 * Use [concurrency] to configure the concurrency number.
 *
 * One thing to note is that the order of the elements is preserved,
 * so the output flow will contain the same elements as the input flow,
 * but with the values transformed according to the provided function.
 */
fun <T, R> Flow<T>.mapAsync(
    concurrency: Int,
    transform: suspend (T) -> R
): Flow<R> = MapAsyncFlow(this, concurrency, transform)

/**
 * The [unorderedMapAsync] function is similar to the [mapAsync] function in that it transforms each element
 * in a parallel way using the provided [f] function. However, unlike [mapAsync], this function does not guarantee
 * that the output elements will be in the same order as the input elements. This means that this function can be
 * significantly faster than [mapAsync] because it does not have to preserve order.
 *
 * Use [concurrency] to configure the concurrency number.
 */
fun <T, R> Flow<T>.unorderedMapAsync(
    concurrency: Int,
    f: suspend (T) -> R
): Flow<R> = UnorderedMapAsyncFlow(this, concurrency, f)

/**
 * The [flatMapAsync] function is similar to the [flatMapIterable] function but works in a parallel way
 * to transform each element of the [Flow] with the provided [f] function.
 *
 * This function transforms each element of the input Flow by applying the [f] function in a
 * parallel way. This means that multiple elements can be processed at the same time, especially useful
 * for more intensive tasks.
 *
 * Use [concurrency] to configure the concurrency number.
 *
 * The output of this function is a Flow of the transformed elements, where the order of the elements is preserved.
 * This means that the output Flow will contain the same elements as the input Flow, but with each element
 * transformed according to the provided function.
 */
fun <T, R> Flow<T>.flatMapAsync(
    concurrency: Int,
    f: suspend (T) -> Iterable<R>
): Flow<R> = mapAsync(concurrency, f).flattenIterable()

/**
 * The [unorderedFlatMapAsync] function is similar to the [flatMapAsync] function but does not guarantee
 * the order of the output elements.
 *
 * This function transforms each element of the input Flow by applying the [f] function in a parallel way.
 * This means that multiple elements can be processed at the same time, especially useful for more intensive tasks.
 *
 * Use [concurrency] to configure the concurrency number.
 *
 * The output of this function is a Flow of the transformed elements, where the order of the elements is not preserved.
 * This means that the output Flow may not contain the elements in the same order as the input Flow.
 * However, this function can be significantly faster than [flatMapAsync] because it does not have to preserve order.
 */
fun <T, R> Flow<Iterable<T>>.unorderedFlatMapAsync(
    concurrency: Int,
    f: suspend (Iterable<T>) -> Iterable<R>
): Flow<R> = unorderedMapAsync(concurrency, f).flattenIterable()

/**
 * Performs the provided [f] action concurrently on each item emitted by the flow. The action
 * is applied with the specified [concurrency].
 *
 * @param concurrency The maximum number of concurrent invocations of the action [f].
 * @param f The action to apply to each item emitted by the flow.
 * @return A [Flow] of items with the action applied concurrently.
 */
fun <T> Flow<T>.onEachAsync(
    concurrency: Int,
    f: suspend (T) -> Unit
): Flow<T> = mapAsync(concurrency) { it.also { f(it) } }

/**
 * Performs the provided [f] action concurrently on each item emitted by the flow. The action
 * is applied with the specified [concurrency]. The order of items might not be preserved.
 *
 * @param concurrency The maximum number of concurrent invocations of the action [f].
 * @param f The action to apply to each item emitted by the flow.
 * @return A [Flow] of items with the action applied concurrently and possibly unordered.
 */
fun <T> Flow<T>.unorderedOnEachAsync(
    concurrency: Int,
    f: suspend (T) -> Unit
): Flow<T> = unorderedMapAsync(concurrency) { it.also { f(it) } }

/**
 * Collects the flow and performs the provided [f] action concurrently on each item emitted by the
 * flow. The action is applied with the specified [concurrency].
 *
 * @param concurrency The maximum number of concurrent invocations of the action [f].
 * @param f The action to apply to each item emitted by the flow.
 */
suspend fun <T> Flow<T>.collectAsync(
    concurrency: Int,
    f: suspend (T) -> Unit
): Unit = onEachAsync(concurrency, f).collect()

/**
 * Collects the flow and performs the provided [f] action concurrently on each item emitted by the
 * flow. The action is applied with the specified [concurrency]. The order of items might
 * not be preserved.
 *
 * @param concurrency The maximum number of concurrent invocations of the action [f].
 * @param f The action to apply to each item emitted by the flow.
 */
suspend fun <T> Flow<T>.unorderedCollectAsync(
    concurrency: Int,
    f: suspend (T) -> Unit
): Unit = unorderedOnEachAsync(concurrency, f).collect()
