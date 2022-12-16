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

fun <T> Flow<T>.windowedChunk(
    size: Int,
    duration: Duration
): Flow<List<T>> =
    chunked(ChunkStrategy.TimeWindow(size, duration))

fun <T> Flow<T>.chunked(strategy: ChunkStrategy): Flow<List<T>> =
    Chunk(this, strategy)

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

fun <T, R> Flow<T>.mapParallel(
    concurrencyLevel: Int,
    f: suspend ConcurrencyInfo.(T) -> R
): Flow<R> = MapParallelFlow(this, concurrencyLevel, f)

fun <T, R> Flow<T>.unorderedMapParallel(
    concurrencyLevel: Int,
    f: suspend ConcurrencyInfo.(T) -> R
): Flow<R> = UnorderedMapParallelFlow(this, concurrencyLevel, f)

fun <T, R> Flow<Iterable<T>>.unorderedFlatMapParallel(
    concurrencyLevel: Int,
    f: suspend ConcurrencyInfo.(Iterable<T>) -> Iterable<R>
): Flow<R> = mapParallel(concurrencyLevel, f).flatten()

fun <T, R> Flow<Iterable<T>>.flatMapParallel(
    concurrencyLevel: Int,
    f: suspend ConcurrencyInfo.(Iterable<T>) -> Iterable<R>
): Flow<R> = mapParallel(concurrencyLevel, f).flatten()

fun <T> Flow<T>.collectAsync(
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    collector: FlowCollector<T> = FlowCollector { }
): Job = scope.launch { collect(collector) }

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

