package io.river.core

import io.river.core.internal.Chunk
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration

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
