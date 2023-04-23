package com.river.core

import com.river.core.internal.Chunk
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration

/**
 * The [chunked] function is used to group the elements emitted by the current [Flow] into chunks based on the provided
 * [ChunkStrategy]. This can be useful for batch processing or aggregating streams of data more efficiently by
 * combining them into larger, more manageable pieces.
 *
 * The chunks are emitted as a [List] of elements, with each chunk containing a maximum number of elements or a time window,
 * depending on the provided [ChunkStrategy] implementation.
 *
 * A good practical example of its usage is I/O processing: by using a custom [ChunkStrategy], you can control the trade-off
 * between processing latency and the granularity of data aggregation, allowing for more efficient I/O processing
 * by potentially reducing the number of I/O operations and the amount of resources required for each operation,
 * taking advantage of batching capabilities that may exist in the underlying I/O systems (e.g., databases, file systems, or network protocols).

 * Note that the chunks may not be of exactly the same size, depending on the number of elements emitted and the
 * specified strategy. However, the order of elements in the output flow is preserved from the input flow.
 *
 * @param strategy The [ChunkStrategy] implementation to be used for chunking the elements.
 *
 * @return A Flow of Lists, where each List represents a chunk of elements from the input flow.
 *
 * Example usage:
 * ```
 * val flow: Flow<Int> = flowOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
 * val chunkSize = 4
 *
 * flow.chunked(ChunkStrategy.Count(chunkSize))
 *     .collect { chunk ->
 *         val sum = chunk.sum()
 *         println("Sum of chunk elements: $sum")
 *     }
 * ```
 *
 * In this example, the input flow contains 10 elements. The chunked function is called with a [ChunkStrategy.Count]
 * implementation, which groups elements into chunks of the specified size (4 in this case).
 */
fun <T> Flow<T>.chunked(strategy: ChunkStrategy): Flow<List<T>> =
    Chunk(this, strategy)

/**
 * The [windowedChunk] function is used to group the elements emitted by the current [Flow] into fixed-size chunks based
 * on a time window. This can be useful for batch processing or aggregating streams of data more efficiently by
 * combining them into larger, more manageable pieces.
 *
 * The chunks are emitted as a [List] of elements, with each chunk containing a maximum of 'size' elements or each chunk
 * lasting no longer than the specified 'duration'.
 *
 * A good practical example of its usage is I/O processing: by using both size and duration, you can control the trade-off
 * between processing latency and the granularity of data aggregation, allowing for more efficient I/O processing
 * by potentially reducing the number of I/O operations and the amount of resources required for each operation,
 * taking advantage of batching capabilities that may exist in the underlying I/O systems (e.g., databases, file systems, or network protocols).
 *
 * Note that the chunks may not be of exactly the same size, depending on the number of elements emitted, the
 * specified size and the time window itself. However, the order of elements in the output flow is preserved from the input flow.
 *
 * @param size The maximum number of elements in each chunk.
 * @param duration The maximum duration of each chunk.
 *
 * @return A [Flow] of [List]s, where each [List] represents a chunk of elements from the input flow.
 *
 * Example usage:
 *
 * ```
 * val flow: Flow<Int> = flowOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
 * val chunkSize = 3
 * val chunkDuration = 100.milliseconds
 *
 * flow.windowedChunk(chunkSize, chunkDuration)
 *     .collect { chunk ->
 *         val sum = chunk.sum()
 *         println("Sum of chunk elements: $sum")
 *     }
 * ```
 *
 * In this example, the input flow contains 10 elements. The [windowedChunk] function is called with a chunk size of 3
 * and a duration of 100 milliseconds. This means that the output flow will emit chunks containing up to 3 elements,
 * or chunks lasting up to 100 milliseconds, whichever comes first.
 */
fun <T> Flow<T>.windowedChunk(
    size: Int,
    duration: Duration
): Flow<List<T>> =
    chunked(ChunkStrategy.TimeWindow(size, duration))

/**
 * The [chunked] function is used to group the elements emitted by the current [Flow] into fixed-size chunks based
 * on the number of elements. This can be useful for batch processing or aggregating streams of data more efficiently by
 * combining them into larger pieces.
 *
 * The chunks are emitted as a [List] of elements, with each chunk containing a maximum of 'size' elements.
 *
 * A good practical example of its usage is I/O processing: by using the specified size, you can control the granularity
 * of data aggregation, allowing for more efficient I/O processing by potentially reducing the number of I/O operations
 * and the amount of resources required for each operation, taking advantage of batching capabilities that may exist
 * in the underlying I/O systems (e.g., databases, file systems, or network protocols).
 *
 * Note that the chunks may not be of exactly the same size, depending on the number of elements emitted and the
 * specified size. However, the order of elements in the output flow is preserved from the input flow.
 *
 * @param size The maximum number of elements in each chunk.
 * @return A Flow of Lists, where each List represents a chunk of elements from the input flow.
 *
 * Example usage:
 * ```
 * val flow: Flow<Int> = flowOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
 * val chunkSize = 4
 *
 * flow.chunked(chunkSize)
 *     .collect { chunk ->
 *         val sum = chunk.sum()
 *         println("Sum of chunk elements: $sum")
 *     }
 * ```
 *
 * In this example, the input flow contains 10 elements. The chunked function is called with a chunk size of 4,
 * meaning that the output flow will emit chunks containing up to 4 elements. By grouping data into chunks for batch
 * processing, you can improve I/O efficiency and better utilize system resources.
 */
fun <T> Flow<T>.chunked(size: Int): Flow<List<T>> =
    chunked(ChunkStrategy.Count(size))
