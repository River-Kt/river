package com.river.core

import com.river.core.internal.SplitFlow
import kotlinx.coroutines.flow.*
import kotlin.time.Duration

/**
 * The [split] function is used to group the elements emitted by the current [Flow] into smaller Flows based on the provided
 * [GroupStrategy]. This function is useful when smaller Flows are preferred over the original one, some scenarios include
 * file processing, database operations, and so on.
 *
 * The smaller streams are emitted as [Flow] of elements, with each group containing a maximum number of elements or a time window,
 * depending on the provided [GroupStrategy] implementation.
 *
 * The available strategies include:
 *  1. [GroupStrategy.Count]: Groups the input flow based on the number of items in each chunk.
 *  2. [GroupStrategy.TimeWindow]: Groups the input flow based on a specified time duration.
 *
 * Note that the streams may not be of exactly the same size, depending on the number of elements emitted and the
 * specified strategy. However, the order of elements in the output flows is preserved from the input flow.
 *
 * @param strategy The [GroupStrategy] implementation to be used for grouping the elements.
 *
 * @return A [Flow] of [Flow]s, where each one represents the smaller [Flow]s of elements from the original flow.
 *
 * Example usage:
 * ```
 * val flow: Flow<Int> = flowOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
 * val chunkSize = 4
 *
 * flow.split(GroupStrategy.Count(chunkSize))
 *     .collectIndexed { index, smallerFlow ->
 *         print("Flow #${index + 1}: ")
 *
 *         smallerFlow
 *             .intersperse(", ")
 *             .collect(::print)
 *
 *         println()
 *     }
 *
 *  // Output:
 *  // Flow #1: 1, 2, 3, 4
 *  // Flow #2: 5, 6, 7, 8
 *  // Flow #3: 9, 10
 * ```
 *
 *  In this example, the input flow contains 10 elements. The [split] function is called with a size of 4. This means that
 *  the output flow will emit the [Flow]s, each one containing at most 4 elements.
 */
fun <T> Flow<T>.split(
    strategy: GroupStrategy
): Flow<Flow<T>> = SplitFlow(this, strategy)

/**
 * The [split] function is used to group the elements emitted by the current [Flow] into smaller Flows based a max number of items.
 * This function is useful when smaller Flows are preferred over the original one, some scenarios include file processing, database operations, and so on.
 *
 * The smaller streams are emitted as [Flow] of elements, with each group containing a maximum number of elements.
 *
 * Note that the streams may not be of exactly the same size, depending on the number of elements emitted. However,
 * the order of elements in the output flows is preserved from the input flow.
 *
 * @param size The maximum number of elements in each [Flow].
 *
 * @return A [Flow] of [Flow]s, where each one represents the smaller [Flow]s of elements from the original flow.
 *
 * Example usage:
 * ```
 * val flow: Flow<Int> = flowOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
 * val splitSize = 4
 *
 * flow.split(splitSize)
 *     .collectIndexed { index, smallerFlow ->
 *         print("Flow #${index + 1}: ")
 *
 *         smallerFlow
 *             .intersperse(", ")
 *             .collect(::print)
 *
 *         println()
 *     }
 *
 *  // Output:
 *  // Flow #1: 1, 2, 3, 4
 *  // Flow #2: 5, 6, 7, 8
 *  // Flow #3: 9, 10
 * ```
 *
 *  In this example, the input flow contains 10 elements. The [split] function is called with a size of 4. This means that
 *  the output flow will emit the [Flow]s, each one containing at most 4 elements.
 */
fun <T> Flow<T>.split(size: Int): Flow<Flow<T>> =
    split(GroupStrategy.Count(size))

/**
 * The [split] function is used to group the elements emitted by the current [Flow] into smaller Flows based on size and timeout duration.
 * This function is useful when smaller Flows are preferred over the original one, some scenarios include file processing, database operations, and so on.
 *
 * The smaller streams are emitted as [Flow] of elements, with each group containing a maximum number of elements or a time window.
 *
 * Note that the streams may not be of exactly the same size, depending on the number of elements emitted, the timeout, and the
 * specified strategy. However, the order of elements in the output flows is preserved from the input flow.
 *
 * @param size The maximum number of elements in each [Flow].
 * @param duration The maximum duration of each smaller [Flow].
 *
 * @return A [Flow] of [Flow]s, where each one represents the smaller [Flow]s of elements from the original flow.
 *
 * Example usage:
 * ```
 * val flow: Flow<Int> = flowOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
 * val splitSize = 3
 *
 * flow.split(splitSize, 100.milliseconds)
 *     .collectIndexed { index, smallerFlow ->
 *         print("Flow #${index + 1}: ")
 *
 *         smallerFlow
 *             .intersperse(", ")
 *             .collect(::print)
 *
 *         println()
 *     }
 * ```
 *
 * In this example, the input flow contains 10 elements. The [split] function is called with a size of 3
 * and a duration of 100 milliseconds. This means that the output flow will emit the [Flow]s containing up to 3 elements,
 * or lasting up to 100 milliseconds, whichever comes first.
 *
 */
fun <T> Flow<T>.split(size: Int, duration: Duration): Flow<Flow<T>> =
    split(GroupStrategy.TimeWindow(size, duration))

/**
 * The [chunked] function is used to group the elements emitted by the current [Flow] into chunks based on the provided
 * [GroupStrategy]. This can be useful for batch processing or aggregating streams of data more efficiently by
 * combining them into larger pieces.
 *
 * The chunks are emitted as a [List] of elements, with each chunk containing a maximum number of elements or a time window,
 * depending on the provided [GroupStrategy] implementation.
 *
 * The available strategies include:
 *  1. [GroupStrategy.Count]: Groups the input flow based on the number of items in each chunk.
 *  2. [GroupStrategy.TimeWindow]: Groups the input flow based on a specified time duration.
 *
 * A good practical example of its usage is I/O processing: by using a custom [GroupStrategy], you can control the trade-off
 * between processing latency and the granularity of data aggregation, allowing for more efficient I/O processing
 * by potentially reducing the number of I/O operations and the amount of resources required for each operation,
 * taking advantage of batching capabilities that may exist in the underlying I/O systems (e.g., databases, file systems, or network protocols).

 * Note that the chunks may not be of exactly the same size, depending on the number of elements emitted and the
 * specified strategy. However, the order of elements in the output flow is preserved from the input flow.
 *
 * @param strategy The [GroupStrategy] implementation to be used for chunking the elements.
 *
 * @return A Flow of Lists, where each List represents a chunk of elements from the input flow.
 *
 * Example usage:
 * ```
 * val flow: Flow<Int> = flowOf(4, 4, 4, 4, 5, 5, 5, 5, 6, 6, 6)
 * val chunkSize = 4
 *
 * flow.chunked(GroupStrategy.Count(chunkSize))
 *     .collectIndexed { index, chunk ->
 *         println("Sum of chunk #${index + 1}: ${chunk.sum()}")
 *     }
 *
 *  // Output:
 *  // Sum of chunk #1: 16
 *  // Sum of chunk #2: 20
 *  // Sum of chunk #3: 18
 * ```
 *
 * In this example, the input flow contains 11 elements. The chunked function is called with a chunk size of 4,
 * meaning that the output flow will emit chunks containing up to 4 elements.
 *
 */
fun <T> Flow<T>.chunked(strategy: GroupStrategy): Flow<List<T>> =
    split(strategy).map { it.toList() }

/**
 * The [chunked] function is used to group the elements emitted by the current [Flow] into fixed-size chunks based
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
 *
 * flow.chunked(chunkSize, 100.milliseconds)
 *     .collectIndexed { index, smallerFlow ->
 *         print("Flow #${index + 1}: ")
 *
 *         smallerFlow
 *             .intersperse(", ")
 *             .collect(::print)
 *
 *         println()
 *     }
 * ```
 *
 * In this example, the input flow contains 10 elements. The [chunked] function is called with a chunk size of 3
 * and a duration of 100 milliseconds. This means that the output flow will emit chunks containing up to 3 elements,
 * or chunks lasting up to 100 milliseconds, whichever comes first.
 */
fun <T> Flow<T>.chunked(
    size: Int,
    duration: Duration
): Flow<List<T>> =
    chunked(GroupStrategy.TimeWindow(size, duration))

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
 * val flow: Flow<Int> = flowOf(4, 4, 4, 4, 5, 5, 5, 5, 6, 6, 6)
 * val chunkSize = 4
 *
 * flow.chunked(chunkSize)
 *     .collectIndexed { index, chunk ->
 *         println("Sum of chunk #${index + 1}: ${chunk.sum()}")
 *     }
 *
 *  // Output:
 *  // Sum of chunk #1: 16
 *  // Sum of chunk #2: 20
 *  // Sum of chunk #3: 18
 * ```
 *
 * In this example, the input flow contains 11 elements. The chunked function is called with a chunk size of 4,
 * meaning that the output flow will emit chunks containing up to 4 elements.
 */
fun <T> Flow<T>.chunked(size: Int): Flow<List<T>> =
    chunked(GroupStrategy.Count(size))
