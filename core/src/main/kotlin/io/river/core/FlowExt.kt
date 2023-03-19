@file:OptIn(FlowPreview::class)

package io.river.core

import io.river.core.internal.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlin.time.Duration

fun <T> Flow<T>.timeout(duration: Duration): Flow<T> =
    flow { withTimeoutOrNull(duration) { collect { emit(it) } } }

fun <T> stoppableFlow(block: suspend StoppableFlowCollector<T>.() -> Unit): Flow<T> =
    StoppableFlow { block(StoppableFlowCollector(this)) }

suspend fun <T> Flow<T>.joinToString(
    f: suspend (T) -> String = { it.toString() }
): String =
    map(f).fold("") { acc, element -> acc + element }

suspend fun <T> Flow<T>.joinToString(
    between: String,
    f: suspend (T) -> String
): String =
    map(f)
        .intersperse(between)
        .fold("") { acc, element -> acc + element }

suspend fun <T> Flow<T>.joinToString(
    start: String,
    between: String,
    end: String,
    f: suspend (T) -> String
): String =
    map(f)
        .intersperse(start, between, end)
        .fold("") { acc, element -> acc + element }

fun <T> Flow<T>.intersperse(
    between: T
): Flow<T> = intersperse(start = null, between = between, end = null)

fun <T> Flow<T>.intersperse(
    start: T? = null,
    between: T,
    end: T? = null
): Flow<T> =
    flow {
        var first = true
        var last = false

        if (start != null) emit(start)

        onCompletion {
            last = true
            if (end != null) emit(end)
        }.collect {
            if (!first && !last) emit(between)
            emit(it)
            first = false
        }
    }

fun <T> Flow<T>.delay(duration: Duration): Flow<T> =
    onEach { kotlinx.coroutines.delay(duration) }

fun <T> Flow<T>.throttle(
    elementsPerInterval: Int,
    interval: Duration,
    strategy: ThrottleStrategy = ThrottleStrategy.Suspend
): Flow<T> = ThrottleFlow(elementsPerInterval, interval, strategy, this)

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

suspend fun <T> Flow<T>.toList(size: Int): List<T> =
    take(size).toList()

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
 * The [chunked] function is used to split the elements emitted by the current [Flow] into chunks.
 *
 * The chunks are emitted as a [List] of elements, with each chunk containing size elements or each chunk lasting
 * duration time, depending on the [ChunkStrategy] provided.
 **
 * Note that the chunks may not be of exactly the same size, depending on the number of elements emitted and the
 * specified size. Additionally, the order of elements in the output flow is preserved from the input flow.
 */
fun <T> Flow<T>.chunked(strategy: ChunkStrategy): Flow<List<T>> =
    Chunk(this, strategy)

/**
 * The [windowedChunk] function is used to split the elements emitted by the current [Flow] into fixed size chunks based
 * on a time window.
 *
 * The chunks are emitted as a [List] of elements, with each chunk containing size elements or each chunk lasting
 * duration time.
 *
 * Note that the chunks may not be of exactly the same size, depending on the number of elements emitted and the
 * specified size. Additionally, the order of elements in the output flow is preserved from the input flow.
 */
fun <T> Flow<T>.windowedChunk(
    size: Int,
    duration: Duration
): Flow<List<T>> =
    chunked(ChunkStrategy.TimeWindow(size, duration))

/**
 * The [chunked] function is used to split the elements emitted by the current [Flow] into chunks of a fixed size.
 *
 * The chunks are emitted as a [List] of elements, with each chunk containing size elements.
 *
 * Note that the chunks may not be of exactly the same size, depending on the number of elements emitted and the
 * specified size. Additionally, the order of elements in the output flow is preserved from the input flow.
 */
fun <T> Flow<T>.chunked(size: Int): Flow<List<T>> =
    chunked(ChunkStrategy.Count(size))

fun <T> Flow<Iterable<T>>.flatten(): Flow<T> =
    flatMapConcat { it.asFlow() }

inline fun <T> Flow<T>.onEachParallel(
    concurrencyLevel: Int,
    crossinline f: suspend ConcurrencyInfo.(T) -> Unit
): Flow<T> = mapParallel(concurrencyLevel) { it.also { f(it) } }

inline fun <T> Flow<T>.unorderedOnEachParallel(
    concurrencyLevel: Int,
    crossinline f: suspend ConcurrencyInfo.(T) -> Unit
): Flow<T> = unorderedMapParallel(concurrencyLevel) { it.also { f(it) } }

suspend inline fun <T> Flow<T>.collectParallel(
    concurrencyLevel: Int,
    crossinline f: suspend ConcurrencyInfo.(T) -> Unit
): Unit = onEachParallel(concurrencyLevel, f).collect()

suspend inline fun <T> Flow<T>.unorderedCollectParallel(
    concurrencyLevel: Int,
    crossinline f: suspend ConcurrencyInfo.(T) -> Unit
): Unit = unorderedOnEachParallel(concurrencyLevel, f).collect()

suspend fun <T> Flow<T>.countOnWindow(duration: Duration): Int {
    var counter = 0
    return withTimeoutOrNull(duration) { collect { counter++ }; counter } ?: counter
}

/**
 * The [mapParallel] function is similar to the [map] function
 * since it transforms each element via the [transform] function.
 *
 * It works, however, in a parallel way, which means that multiple elements can be processed at the same time,
 * especially useful for more intensive tasks.
 *
 * Use [concurrencyLevel] to configure the parallelism number.
 *
 * One thing to note is that the order of the elements is preserved,
 * so the output flow will contain the same elements as the input flow,
 * but with the values transformed according to the provided function.
 */
fun <T, R> Flow<T>.mapParallel(
    concurrencyLevel: Int,
    transform: suspend ConcurrencyInfo.(T) -> R
): Flow<R> = MapParallelFlow(this, concurrencyLevel, transform)

/**
 * The [unorderedMapParallel] function is similar to the [mapParallel] function in that it transforms each element
 * in a parallel way using the provided [f] function. However, unlike [mapParallel], this function does not guarantee
 * that the output elements will be in the same order as the input elements. This means that this function can be
 * significantly faster than [mapParallel] because it does not have to preserve order.
 *
 * Use [concurrencyLevel] to configure the parallelism number.
 */
fun <T, R> Flow<T>.unorderedMapParallel(
    concurrencyLevel: Int,
    f: suspend ConcurrencyInfo.(T) -> R
): Flow<R> = UnorderedMapParallelFlow(this, concurrencyLevel, f)

/**
 * The [flatMapParallel] function is similar to the [flatMap] function but works in a parallel way
 * to transform each element of the [Flow], which is an Iterable, with the provided [f] function.
 *
 * This function transforms each Iterable element of the input Flow by applying the [f] function in a
 * parallel way. This means that multiple elements can be processed at the same time, especially useful
 * for more intensive tasks.
 *
 * Use [concurrencyLevel] to configure the parallelism number.
 *
 * The output of this function is a Flow of the transformed elements, where the order of the elements is preserved.
 * This means that the output Flow will contain the same elements as the input Flow, but with each element
 * transformed according to the provided function.
 */
fun <T, R> Flow<Iterable<T>>.flatMapParallel(
    concurrencyLevel: Int,
    f: suspend ConcurrencyInfo.(Iterable<T>) -> Iterable<R>
): Flow<R> = mapParallel(concurrencyLevel, f).flatten()

/**
 * The [unorderedFlatMapParallel] function is similar to the [flatMapParallel] function but does not guarantee
 * the order of the output elements.
 *
 * This function transforms each Iterable element of the input Flow by applying the [f] function in a parallel way.
 * This means that multiple elements can be processed at the same time, especially useful for more intensive tasks.
 *
 * Use [concurrencyLevel] to configure the parallelism number.
 *
 * The output of this function is a Flow of the transformed elements, where the order of the elements is not preserved.
 * This means that the output Flow may not contain the elements in the same order as the input Flow.
 * However, this function can be significantly faster than [flatMapParallel] because it does not have to preserve order.
 */
fun <T, R> Flow<Iterable<T>>.unorderedFlatMapParallel(
    concurrencyLevel: Int,
    f: suspend ConcurrencyInfo.(Iterable<T>) -> Iterable<R>
): Flow<R> = mapParallel(concurrencyLevel, f).flatten()

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

fun <T> Flow<T>.catchAndEmitLast(
    f: FlowCollector<T>.(Throwable) -> T
): Flow<T> =
    catch { emit(f(this, it)) }

suspend fun <T> Flow<T>.collectWithTimeout(
    duration: Duration,
    collector: FlowCollector<T> = FlowCollector { },
): Unit = withTimeout(duration) { collect(collector) }

fun <T> Flow<T>.broadcast(
    number: Int,
    buffer: Int = Channel.BUFFERED,
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
): List<Flow<T>> = Broadcast(scope, buffer, this, number).flows()

fun <E, F, S> Flow<E>.broadcast(
    firstFlowMap: Flow<E>.() -> Flow<F>,
    secondFlowMap: Flow<E>.() -> Flow<S>,
    buffer: Int = Channel.BUFFERED,
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
): Flow<Pair<F, S>> =
    broadcast(2, buffer, scope).let { (first, second) ->
        firstFlowMap(first)
            .zip(secondFlowMap(second)) { f, s -> f to s }
    }

fun <E, F, S, T> Flow<E>.broadcast(
    firstFlowMap: Flow<E>.() -> Flow<F>,
    secondFlowMap: Flow<E>.() -> Flow<S>,
    thirdFlowMap: Flow<E>.() -> Flow<T>,
    buffer: Int = Channel.BUFFERED,
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
): Flow<Triple<F, S, T>> =
    broadcast(3, buffer, scope).let { (first, second, third) ->
        firstFlowMap(first)
            .zip(secondFlowMap(second)) { f, s -> f to s }
            .zip(thirdFlowMap(third)) { (f, s), t -> Triple(f, s, t) }
    }

fun <T> flowOf(item: suspend () -> T) = flow { emit(item()) }

fun <T> repeat(item: T): Flow<T> =
    flow {
        while (true) {
            emit(item)
        }
    }

inline fun <T, R> Flow<T>.via(flow: Flow<T>.() -> Flow<R>) = flow(this)

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

operator fun <T, R : T> Flow<T>.plus(other: Flow<R>) =
    flowOf(this, other)
        .flattenConcat()

