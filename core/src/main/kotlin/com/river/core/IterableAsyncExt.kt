package com.river.core

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
infix fun <T> List<CompletableDeferred<T>>.completeAllWith(result: Result<List<T>>): Boolean =
    result
        .mapCatching { this completeAll it }
        .getOrElse { e -> map { it.completeExceptionally(e) }.all { it } }

/**
 * Transforms the elements of the iterable concurrently using the provided [f] function, and then
 * flattens the result.
 *
 * @param f A suspend function to apply to each element of the iterable.
 * @return A [List] containing the flattened results of applying [f] to each element of the iterable.
 */
suspend fun <T, R> Iterable<T>.flatMapAsync(f: suspend (T) -> Iterable<R>): List<R> =
    mapAsync { f(it) }.flatten()

/**
 * Transforms the elements of the iterable concurrently using the provided [f] function with a specified
 * concurrency limit, and then flattens the result.
 *
 * @param concurrency The maximum number of concurrent transformations.
 * @param f A suspend function to apply to each element of the iterable.
 * @return A [List] containing the flattened results of applying [f] to each element of the iterable.
 */
suspend fun <T, R> Iterable<T>.flatMapAsync(
    concurrency: Int,
    f: suspend (T) -> Iterable<R>
): List<R> = mapAsync(concurrency) { f(it) }.flatten()

/**
 * Transforms the elements of the iterable concurrently using the provided [f] function.
 *
 * @param f A suspend function to apply to each element of the iterable.
 * @return A [List] containing the results of applying [f] to each element of the iterable.
 */
suspend fun <T, R> Iterable<T>.mapAsync(f: suspend (T) -> R): List<R> =
    coroutineScope { map { async { f(it) } }.awaitAll() }

/**
 * Transforms the elements of the iterable concurrently using the provided [f] function with a specified
 * concurrency limit.
 *
 * @param concurrency The maximum number of concurrent transformations.
 * @param f A suspend function to apply to each element of the iterable.
 * @return A [List] containing the results of applying [f] to each element of the iterable.
 */
suspend fun <T, R> Iterable<T>.mapAsync(
    concurrency: Int,
    f: suspend (T) -> R
): List<R> = asFlow().mapAsync(concurrency, f).toList()
