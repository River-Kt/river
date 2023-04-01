package io.river.core

import io.river.core.internal.Broadcast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.zip

/**
 * Creates a list of [number] [Flow]s that share the elements of the original [Flow].
 *
 * @param number The number of [Flow]s to create.
 * @param buffer The buffer size for each [Flow].
 * @param scope The [CoroutineScope] for managing coroutines.
 *
 * @return A list of [Flow]s that share the elements of the original [Flow].
 *
 * Example usage:
 *
 * ```
 *  val sourceFlow = flowOf(1, 2, 3)
 *  val broadcastFlows = sourceFlow.broadcast(2)
 *  // Both broadcastFlows[0] and broadcastFlows[1] will emit 1, 2, 3
 * ```
 */
fun <T> Flow<T>.broadcast(
    number: Int,
    buffer: Int = Channel.BUFFERED,
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
): List<Flow<T>> = Broadcast(scope, buffer, this, number).flows()

/**
 * Broadcasts the original [Flow] into two separate [Flow]s, transforms each of them using the provided
 * mapping functions, and then zips the results back into a single [Flow] of [Pair]s.
 *
 * @param firstFlowMap A lambda expression defining the transformation for the first [Flow].
 * @param secondFlowMap A lambda expression defining the transformation for the second [Flow].
 * @param buffer The buffer size for each [Flow].
 * @param scope The [CoroutineScope] for managing coroutines.
 *
 * @return A new [Flow] of [Pair]s containing the zipped results of the transformed [Flow]s.
 *
 * Example usage:
 *
 * ```
 *  val sourceFlow = flowOf(1, 2, 3)
 *  val combinedFlow = sourceFlow.broadcast(
 *      { map { it * 2 } },
 *      { map { it * 3 } }
 *  )
 *  // Output: (2, 3), (4, 6), (6, 9)
 * ```
 */
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

/**
 * Broadcasts the original [Flow] into three separate [Flow]s, transforms each of them using the provided
 * mapping functions, and then zips the results back into a single [Flow] of [Triple]s.
 *
 * @param firstFlowMap A lambda expression defining the transformation for the first [Flow].
 * @param secondFlowMap A lambda expression defining the transformation for the second [Flow].
 * @param thirdFlowMap A lambda expression defining the transformation for the third [Flow].
 * @param buffer The buffer size for each [Flow].
 * @param scope The [CoroutineScope] for managing coroutines.
 *
 * @return A new [Flow] of [Triple]s containing the zipped results of the transformed [Flow]s.
 *
 * Example usage:
 *
 * ```
 *  val sourceFlow = flowOf(1, 2, 3)
 *  val combinedFlow = sourceFlow.broadcast(
 *      { map { it * 2 } },
 *      { map { it * 3 } },
 *      { map { it * 4 } }
 *  )
 *  // Output: (2, 3, 4), (4, 6, 8), (6, 9, 12)
 *
 * ```
 */
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
