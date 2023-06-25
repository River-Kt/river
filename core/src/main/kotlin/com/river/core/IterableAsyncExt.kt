package com.river.core

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList

/**
 * Completes all the [CompletableDeferred] objects in the list with the corresponding values from the
 * [values] list.
 *
 * @param values A list of values to complete the [CompletableDeferred] objects with.
 * @return A [Boolean] indicating if all the [CompletableDeferred] objects were completed successfully.
 *
 * @throws AssertionError if the sizes of the lists don't match.
 */
@FlowPreview
infix fun <T> List<CompletableDeferred<T>>.completeAll(values: List<T>): Boolean =
    assert(size == values.size) { "Promises & values sizes must match" }
        .let {
            (this zip values)
                .map { (promise, value) -> promise.complete(value) }
                .all { it }
        }

/**
 * Completes all the [CompletableDeferred] objects in the list with the values from the [result] if it
 * is successful or completes them exceptionally with the error from the [result] if it is a failure.
 *
 * @param result A [Result] containing a list of values to complete the [CompletableDeferred] objects with.
 * @return A [Boolean] indicating if all the [CompletableDeferred] objects were completed successfully.
 *
 * @throws AssertionError if the sizes of the lists don't match.
 */
@FlowPreview
infix fun <T> List<CompletableDeferred<T>>.completeAllWith(result: Result<List<T>>): Boolean =
    result
        .mapCatching { this completeAll it }
        .getOrElse { e -> map { it.completeExceptionally(e) }.all { it } }

/**
 * Transforms the elements of the iterable concurrently using the provided [transform] function, and then
 * flattens the result.
 *
 * @param transform A suspend function to apply to each element of the iterable.
 * @return A [List] containing the flattened results of applying [transform] to each element of the iterable.
 */
suspend fun <T, R> Iterable<T>.flatMapIterableAsync(transform: suspend (T) -> Iterable<R>): List<R> =
    mapAsync { transform(it) }.flatten()

/**
 * Transforms the elements of the iterable concurrently using the provided [transform] function with a specified
 * concurrency limit, and then flattens the result.
 *
 * @param concurrency The maximum number of concurrent transformations.
 * @param transform A suspend function to apply to each element of the iterable.
 * @return A [List] containing the flattened results of applying [transform] to each element of the iterable.
 */
suspend fun <T, R> Iterable<T>.flatMapIterableAsync(
    concurrency: Int,
    transform: suspend (T) -> Iterable<R>
): List<R> = mapAsync(concurrency) { transform(it) }.flatten()

/**
 * Transforms the elements of the iterable concurrently using the provided [transform] function.
 *
 * @param transform A suspend function to apply to each element of the iterable.
 * @return A [List] containing the results of applying [transform] to each element of the iterable.
 */
suspend fun <T, R> Iterable<T>.mapAsync(transform: suspend (T) -> R): List<R> =
    coroutineScope { map { async { transform(it) } }.awaitAll() }

/**
 * Transforms the elements of the iterable concurrently using the provided [transform] function with a specified
 * concurrency limit.
 *
 * @param concurrency The maximum number of concurrent transformations.
 * @param transform A suspend function to apply to each element of the iterable.
 * @return A [List] containing the results of applying [transform] to each element of the iterable.
 */
suspend fun <T, R> Iterable<T>.mapAsync(
    concurrency: Int,
    transform: suspend (T) -> R
): List<R> = asFlow().mapAsync(concurrency, transform).toList()
