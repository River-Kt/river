package io.github.gabfssilva.river.core

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList

infix fun <T> List<CompletableDeferred<T>>.completeAll(values: List<T>): Boolean =
    assert(size == values.size) { "Promises & values sizes must match" }
        .let {
            (this zip values)
                .map { (promise, value) -> promise.complete(value) }
                .all { it }
        }

infix fun <T> List<CompletableDeferred<T>>.completeAllWith(result: Result<List<T>>): Boolean =
    result
        .mapCatching { this completeAll it }
        .getOrElse { e -> map { it.completeExceptionally(e) }.all { it } }

suspend fun <T, R> Iterable<T>.flatMapParallel(f: suspend (T) -> Iterable<R>): List<R> =
    mapParallel { f(it) }.flatten()

suspend fun <T, R> Iterable<T>.flatMapParallel(
    concurrency: Int,
    f: suspend (T) -> Iterable<R>
): List<R> = mapParallel(concurrency) { f(it) }.flatten()

suspend fun <T, R> Iterable<T>.mapParallel(f: suspend (T) -> R): List<R> =
    coroutineScope { map { async { f(it) } }.awaitAll() }

suspend fun <T, R> Iterable<T>.mapParallel(
    concurrency: Int,
    f: suspend ConcurrencyInfo.(T) -> R
): List<R> = asFlow().mapParallel(concurrency, f).toList()
