@file:OptIn(FlowPreview::class)

package com.river.core

import com.river.core.internal.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlin.time.Duration

/**
 * Applies a timeout to the [Flow]. If the flow does not complete within the specified [duration],
 * it will be cancelled.
 *
 * @param duration The maximum duration the flow is allowed to run before being cancelled.
 * @return A [Flow] with a timeout applied.
 */
fun <T> Flow<T>.timeout(duration: Duration): Flow<T> =
    flow { withTimeoutOrNull(duration) { collect { emit(it) } } }

/**
 * Introduces a delay between the emissions of the [Flow].
 *
 * @param duration The duration of the delay between emissions.
 * @return A [Flow] with a delay applied between emissions.
 */
fun <T> Flow<T>.delay(duration: Duration): Flow<T> =
    onEach { kotlinx.coroutines.delay(duration) }

/**
 * Collects the specified number of items [size] from the [Flow] into a [List].
 *
 * @param size The maximum number of items to collect.
 * @return A [List] of collected items, with a maximum size of [size].
 */
suspend fun <T> Flow<T>.toList(size: Int): List<T> =
    take(size).toList()

/**
 * Collects the specified number of items [size] from the [Flow] into a [List] within
 * the given [duration].
 *
 * @param size The maximum number of items to collect.
 * @param duration The maximum duration allowed for collecting items.
 * @return A [List] of collected items, with a maximum size of [size].
 */
suspend fun <T> Flow<T>.toList(
    size: Int,
    duration: Duration
): List<T> =
    stoppableFlow {
        var counter = 0

        withTimeoutOrNull(duration) {
            collect {
                if (++counter <= size) emit(it)
                else halt()
            }
        }

        halt()
    }.toList()

/**
 * Flattens a [Flow] of [Iterable] items into a [Flow] of individual items.
 *
 * @return A [Flow] of individual items.
 */
fun <T> Flow<Iterable<T>>.flatten(): Flow<T> =
    flatMapConcat { it.asFlow() }

/**
 * Performs the provided [f] action concurrently on each item emitted by the flow. The action
 * is applied with the specified [concurrency].
 *
 * @param concurrency The maximum number of concurrent invocations of the action [f].
 * @param f The action to apply to each item emitted by the flow.
 * @return A [Flow] of items with the action applied concurrently.
 */
inline fun <T> Flow<T>.onEachAsync(
    concurrency: Int,
    crossinline f: suspend ConcurrencyInfo.(T) -> Unit
): Flow<T> = mapAsync(concurrency) { it.also { f(it) } }

/**
 * Performs the provided [f] action concurrently on each item emitted by the flow. The action
 * is applied with the specified [concurrency]. The order of items might not be preserved.
 *
 * @param concurrency The maximum number of concurrent invocations of the action [f].
 * @param f The action to apply to each item emitted by the flow.
 * @return A [Flow] of items with the action applied concurrently and possibly unordered.
 */
inline fun <T> Flow<T>.unorderedOnEachAsync(
    concurrency: Int,
    crossinline f: suspend ConcurrencyInfo.(T) -> Unit
): Flow<T> = unorderedMapAsync(concurrency) { it.also { f(it) } }

/**
 * Collects the flow and performs the provided [f] action concurrently on each item emitted by the
 * flow. The action is applied with the specified [concurrency].
 *
 * @param concurrency The maximum number of concurrent invocations of the action [f].
 * @param f The action to apply to each item emitted by the flow.
 */
suspend inline fun <T> Flow<T>.collectAsync(
    concurrency: Int,
    crossinline f: suspend ConcurrencyInfo.(T) -> Unit
): Unit = onEachAsync(concurrency, f).collect()

/**
 * Collects the flow and performs the provided [f] action concurrently on each item emitted by the
 * flow. The action is applied with the specified [concurrency]. The order of items might
 * not be preserved.
 *
 * @param concurrency The maximum number of concurrent invocations of the action [f].
 * @param f The action to apply to each item emitted by the flow.
 */
suspend inline fun <T> Flow<T>.unorderedCollectAsync(
    concurrency: Int,
    crossinline f: suspend ConcurrencyInfo.(T) -> Unit
): Unit = unorderedOnEachAsync(concurrency, f).collect()

/**
 * Counts the number of items emitted by the flow within the specified [duration] window.
 *
 * @param duration The duration of the counting window.
 * @return The number of items emitted by the flow within the specified duration.
 */
suspend fun <T> Flow<T>.countOnWindow(duration: Duration): Int {
    var counter = 0
    return withTimeoutOrNull(duration) { collect { counter++ }; counter } ?: counter
}

/**
 * The [mapAsync] function is similar to the [map] function
 * since it transforms each element via the [transform] function.
 *
 * It works, however, asynchronously, which means that multiple elements can be processed at the same time,
 * especially useful for more intensive tasks.
 *
 * Use [concurrency] to configure the concurrency number.
 *
 * One thing to note is that the order of the elements is preserved,
 * so the output flow will contain the same elements as the input flow,
 * but with the values transformed according to the provided function.
 */
fun <T, R> Flow<T>.mapAsync(
    concurrency: Int,
    transform: suspend ConcurrencyInfo.(T) -> R
): Flow<R> = MapAsyncFlow(this, concurrency, transform)

/**
 * The [unorderedMapAsync] function is similar to the [mapAsync] function in that it transforms each element
 * iasynchronously using the provided [f] function. However, unlike [mapAsync], this function does not guarantee
 * that the output elements will be in the same order as the input elements. This means that this function can be
 * significantly faster than [mapAsync] because it does not have to preserve order.
 *
 * Use [concurrency] to configure the maximum number of concurrent coroutines that can be executed.
 */
fun <T, R> Flow<T>.unorderedMapAsync(
    concurrency: Int,
    f: suspend ConcurrencyInfo.(T) -> R
): Flow<R> = UnorderedMapAsyncFlow(this, concurrency, f)

/**
 * The [flatMapAsync] function is similar to the [flatMap] function but works asynchronously
 * to transform each element of the [Flow], which is an Iterable, with the provided [f] function.
 *
 * This function transforms each Iterable element of the input Flow by applying the [f] function asynchronously.
 * This means that multiple elements can be processed at the same time, especially useful for more intensive tasks.
 *
 * Use [concurrency] to configure the maximum number of concurrent coroutines that can be executed.
 *
 * The output of this function is a Flow of the transformed elements, where the order of the elements is preserved.
 * This means that the output Flow will contain the same elements as the input Flow, but with each element
 * transformed according to the provided function.
 */
fun <T, R> Flow<Iterable<T>>.flatMapAsync(
    concurrency: Int,
    f: suspend ConcurrencyInfo.(Iterable<T>) -> Iterable<R>
): Flow<R> = mapAsync(concurrency, f).flatten()

/**
 * The [unorderedFlatMapAsync] function is similar to the [flatMapAsync] function but does not guarantee
 * the order of the output elements.
 *
 * This function transforms each Iterable element of the input Flow by applying the [f] function asynchronously.
 * This means that multiple elements can be processed at the same time, especially useful for more intensive tasks.
 *
 * Use [concurrency] to configure the maximum number of concurrent coroutines that can be executed.
 *
 * The output of this function is a Flow of the transformed elements, where the order of the elements is not preserved.
 * This means that the output Flow may not contain the elements in the same order as the input Flow.
 * However, this function can be significantly faster than [flatMapAsync] because it does not have to preserve order.
 */
fun <T, R> Flow<Iterable<T>>.unorderedFlatMapAsync(
    concurrency: Int,
    f: suspend ConcurrencyInfo.(Iterable<T>) -> Iterable<R>
): Flow<R> = mapAsync(concurrency, f).flatten()

/**
 * The [collectAsync] function launches a coroutine to collect the elements emitted by the current [Flow] in an asynchronous way.
 *
 * This function returns a [Job] instance that represents the coroutine launched to collect the elements.
 *
 * The coroutine will continue running until the flow completes or an exception is thrown.
 *
 * If the caller needs to cancel the coroutine before it completes, they can cancel the returned [Job].
 */
fun <T> Flow<T>.collectAsync(
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    collector: FlowCollector<T> = FlowCollector { }
): Job = scope.launch { collect(collector) }

/**
 * The [collectCatching] function collects the elements emitted by the current [Flow] in a suspending way and returns
 * a [Result] instance that represents the result of the operation.
 *
 * If the collection of elements completes successfully, the function returns a [Result] instance with a value of [Unit].
 * Otherwise, it returns a [Result] instance with the corresponding error.
 */
suspend fun <T> Flow<T>.collectCatching(
    collector: FlowCollector<T> = FlowCollector { },
): Result<Unit> = runCatching { collect(collector) }

/**
 * Catches exceptions that occur while collecting the flow and emits the result of the provided
 * function [f] with the caught exception as a parameter.
 *
 * @param f A function that takes a [FlowCollector] and a [Throwable] and returns a value of type [T].
 * @return A [Flow] that emits the original flow's values and the result of the [f] function in case
 *         of an exception.
 */
fun <T> Flow<T>.catchAndEmitLast(
    f: FlowCollector<T>.(Throwable) -> T
): Flow<T> =
    catch { emit(f(this, it)) }

/**
 * Collects the flow with a specified timeout duration. If the flow takes longer than the provided
 * [duration] to complete, it throws a [TimeoutCancellationException].
 *
 * @param duration The maximum time allowed for the flow collection to complete.
 * @param collector An optional [FlowCollector] for handling the flow's emissions.
 * @throws TimeoutCancellationException if the flow collection takes longer than the specified duration.
 */
suspend fun <T> Flow<T>.collectWithTimeout(
    duration: Duration,
    collector: FlowCollector<T> = FlowCollector { },
): Unit = withTimeout(duration) { collect(collector) }

/**
 * Creates a [Flow] that emits a single item, which is the result of invoking the provided
 * suspending function [item].
 *
 * @param item The suspending function to be invoked when the flow is collected.
 * @return A [Flow] that emits the result of the suspending function.
 */
fun <T> flowOf(item: suspend () -> T) = flow { emit(item()) }

/**
 * Creates an infinite [Flow] that repeatedly emits the provided [item].
 *
 * @param item The item to be repeatedly emitted by the flow.
 * @return An infinite [Flow] that repeatedly emits the provided item.
 */
fun <T> repeat(item: T): Flow<T> =
    flow {
        while (true) {
            emit(item)
        }
    }

/**
 * Allows the [Flow] to be collected and transformed into another [Flow] asynchronously. The
 * transformed [Flow] is collected asynchronously in the provided [scope]. The original flow
 * and the transformed flow share the same buffer with the specified [bufferCapacity],
 * [onBufferOverflow] policy, and [onUndeliveredElement] handler.
 *
 * @param bufferCapacity The capacity of the shared buffer.
 * @param onBufferOverflow The policy to apply when the buffer overflows.
 * @param onUndeliveredElement The function to be invoked when an element cannot be delivered.
 * @param scope The [CoroutineScope] to collect the transformed flow asynchronously.
 * @param flow The transformation function that maps the original flow to a new flow.
 * @return A [Flow] of the original items.
 */
fun <E, S> Flow<E>.alsoTo(
    bufferCapacity: Int = Channel.BUFFERED,
    onBufferOverflow: BufferOverflow = BufferOverflow.SUSPEND,
    onUndeliveredElement: ((E) -> Unit)? = null,
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    flow: Flow<E>.() -> Flow<S>,
): Flow<E> = flow {
    val channel = Channel(bufferCapacity, onBufferOverflow, onUndeliveredElement)
    flow(channel.consumeAsFlow()).collectAsync(scope)

    buffer(bufferCapacity)
        .onCompletion { channel.cancel() }
        .collect {
            channel.send(it)
            emit(it)
        }
}

/**
 * Combines two [Flow]s of the same base type [T] into a single [Flow] by concatenating their elements.
 *
 * @param other The [Flow] to concatenate with the current [Flow].
 *
 * @return A new [Flow] containing the concatenated elements of both the current and the [other] [Flow]s.
 *
 * Example usage:
 *
 * ```
 *  val flow1 = flowOf(1, 2, 3)
 *  val flow2 = flowOf(4, 5, 6)
 *  val combinedFlow = flow1 + flow2 //1, 2, 3, 4, 5, 6
 * ```
 */
operator fun <T, R : T> Flow<T>.plus(other: Flow<R>) =
    flowOf(this, other)
        .flattenConcat()
