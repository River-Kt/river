package com.river.core

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlin.time.Duration

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
): List<T> {
    val acc = mutableListOf<T>()
    withTimeoutOrNull(duration) { take(size).collect { acc.add(it) } }
    return acc
}

/**
 * Collects the items from the [Flow] into a [List] within the given [duration].
 *
 * @param duration The maximum duration allowed for collecting items.
 *
 * @return A [List] of collected items.
 */
suspend fun <T> Flow<T>.toList(duration: Duration): List<T> =
    toList(Int.MAX_VALUE, duration)

/**
 * Flattens a [Flow] of [Flow] items into a [Flow] of individual items.
 *
 * @return A [Flow] of individual items.
 */
fun <T> Flow<Flow<T>>.flattenFlow() =
    flow {
        collect { value -> emitAll(value) }
    }

/**
 * Flattens a [Flow] of [T] items into a [Flow] of individual items [R].
 *
 * @return A [Flow] of individual items.
 */
fun <T, R> Flow<T>.flatMapFlow(mapper: suspend (T) -> Flow<R>): Flow<R> =
    map(mapper).flattenFlow()

/**
 * Flattens a [Flow] of [Iterable] items into a [Flow] of individual items.
 *
 * @return A [Flow] of individual items.
 */
fun <T> Flow<Iterable<T>>.flattenIterable(): Flow<T> =
    flatMapIterable { it }

/**
 * Flattens a [Flow] of [Iterable] items into a [Flow] of individual items.
 *
 * @return A [Flow] of individual items.
 */
fun <T, R> Flow<T>.flatMapIterable(mapper: suspend (T) -> Iterable<R>): Flow<R> =
    flatMapFlow { mapper(it).asFlow() }

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
fun <T> flowOfSuspend(item: suspend () -> T) = flow { emit(item()) }

/**
 * Creates an infinite [Flow] that repeatedly emits the provided [item].
 *
 * @param item The item to be repeatedly emitted by the flow.
 * @return An infinite [Flow] that repeatedly emits the provided item.
 */
fun <T> indefinitelyRepeat(item: T): Flow<T> =
    flow {
        while (true) { emit(item) }
    }

/**
 * Creates an (almost) infinite [Flow] that emits sequentially incremented Long numbers starting from the [startAt] parameter.
 *
 * @param startAt The inclusive starting point.
 * @return An (almost) infinite [Flow] of sequentially incremented Long numbers.
 */
fun unboundedLongFlow(startAt: Long = 0): Flow<Long> =
    flow {
        var number = startAt

        while (true) {
            emit(number++)
        }
    }

/**
 * Allows the [Flow] to be collected and transformed into another [Flow] concurrently. The
 * transformed [Flow] is collected asynchronously in the provided [coroutineScope]. The original flow
 * and the transformed flow share the same buffer with the specified [bufferCapacity],
 * [onBufferOverflow] policy, and [onUndeliveredElement] handler.
 *
 * @param bufferCapacity The capacity of the shared buffer.
 * @param onBufferOverflow The policy to apply when the buffer overflows.
 * @param onUndeliveredElement The function to be invoked when an element cannot be delivered.
 * @param flow The transformation function that maps the original flow to a new flow.
 * @return A [Flow] of the original items.
 */
@ExperimentalRiverApi
fun <E, S> Flow<E>.alsoTo(
    bufferCapacity: Int = Channel.BUFFERED,
    onBufferOverflow: BufferOverflow = BufferOverflow.SUSPEND,
    onUndeliveredElement: ((E) -> Unit)? = null,
    flow: Flow<E>.() -> Flow<S>,
): Flow<Pair<E, S>> =
    flow {
        coroutineScope {
            val channel = Channel(bufferCapacity, onBufferOverflow, onUndeliveredElement)
            val alsoToFlow = flow(channel.consumeAsFlow().buffer(bufferCapacity))

            onEach { channel.send(it) }
                .buffer(bufferCapacity)
                .zip(alsoToFlow) { f, s -> f to s }
                .also { emitAll(it) }
        }
    }

/**
 * Collects the items emitted by this Flow into a ChannelReceiverContext and applies the given block to it.
 *
 * This function launches a new coroutine in the current CoroutineScope in which the given block is executed.
 * The block is provided with a ChannelReceiverContext that it can use to receive items from the Flow.
 * Once all items have been collected, the ChannelReceiverContext is marked as completed and the coroutine is joined.
 *
 * @param block The block to apply to the ChannelReceiverContext.
 *
 * Example usage:
 *
 * ```
 * val flow = flowOf(1, 2, 3, 4, 5)
 *
 * flow.collectAsReceiver {
 *     while (true) {
 *         val items = next(3)
 *         println(items)
 *     }
 *
 *     // Prints: [1, 2, 3] and then [4, 5]
 * }
 * ```
 */
context(CoroutineScope)
@ExperimentalRiverApi
suspend fun <T> Flow<T>.collectAsReceiver(
    block: suspend ChannelReceiverContext<T>.() -> Unit
) {
    val channel = Channel<T>()
    val context = ChannelReceiverContext(channel)
    val job = launch { block(context) }
    toChannel(channel)
    context.markAsCompleted()
    job.join()
}

/**
 * Collects the items emitted by this Flow into a ChannelReceiverContext and applies the given block to it.
 *
 * This function launches a new coroutine in the current CoroutineScope in which the given block is executed.
 * The block is provided with a ChannelReceiverContext that it can use to receive items from the Flow.
 * Once all items have been collected, the ChannelReceiverContext is marked as completed and the coroutine is joined.
 *
 * @param block The block to apply to the ChannelReceiverContext.
 *
 * Example usage:
 *
 * ```
 * val flow = flowOf(1, 2, 3, 4, 5)
 * flow.collectAsReceiver {
 *     while (true) {
 *         val items = next(3)
 *         println(items)
 *     }
 *
 *     // Prints: [1, 2, 3] and then [4, 5]
 * }
 * ```
 */
@ExperimentalRiverApi
suspend fun <T> Flow<T>.collectAsReceiver(
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    block: suspend ChannelReceiverContext<T>.() -> Unit
) = with(scope) { collectAsReceiver(block) }

/**
 * This function collects the items emitted by the Flow and sends each one to the given channel.
 *
 * @param channel The channel to which the items should be sent.
 */
suspend fun <T> Flow<T>.toChannel(
    channel: Channel<T>
): Unit = collect { channel.send(it) }

internal fun CoroutineScope.self() = this

context(CoroutineScope)
fun <T> Flow<T>.launch(): Job = launchIn(self())

context(CoroutineScope)
fun <T> Flow<T>.launchCollect(
    collector: FlowCollector<T> = FlowCollector {}
): Job = launch { collect(collector) }


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
        .flattenFlow()
