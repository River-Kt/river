package com.river.connector.file

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.runningFold
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousFileChannel
import java.nio.channels.CompletionHandler
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Writes the content of the given [src] ByteBuffer to this [AsynchronousFileChannel]
 * at the specified [position] using a coroutine-friendly API.
 *
 * @param src The source ByteBuffer to be written to the channel.
 * @param position The position in the file at which the writing starts.
 *
 * @return The number of bytes written.
 *
 * Example usage:
 *
 * ```
 * val channel: AsynchronousFileChannel = ...
 * val buffer: ByteBuffer = ...
 * val bytesWritten = channel.coWrite(buffer, 0)
 * ```
 */
suspend fun AsynchronousFileChannel.coWrite(
    src: ByteBuffer,
    position: Long,
): Int = suspendCoroutine { continuation ->
    write(
        src,
        position,
        null,
        object : CompletionHandler<Int, Nothing?> {
            override fun completed(result: Int, attachment: Nothing?) {
                continuation.resume(result)
            }

            override fun failed(exc: Throwable, attachment: Nothing?) {
                continuation.resumeWithException(exc)
            }
        }
    )
}

/**
 * Reads content from this [AsynchronousFileChannel] into a [ByteBuffer] of the given [bufferSize]
 * starting at the specified [position] using a coroutine-friendly API.
 *
 * @param bufferSize The size of the buffer to read data into.
 * @param position The position in the file at which the reading starts.
 *
 * @return The ByteBuffer containing the read data.
 *
 * Example usage:
 * ```
 * val channel: AsynchronousFileChannel = ...
 * val buffer = channel.coRead(1024, 0)
 * ```
 */
suspend fun AsynchronousFileChannel.coRead(
    bufferSize: Int,
    position: Long
): ByteBuffer = suspendCoroutine { continuation ->
    val buffer = ByteBuffer.allocate(bufferSize)

    read(
        buffer,
        position,
        buffer,
        object : CompletionHandler<Int, ByteBuffer> {
            override fun completed(result: Int, attachment: ByteBuffer) {
                continuation.resume(attachment)
            }

            override fun failed(exc: Throwable, attachment: ByteBuffer?) {
                continuation.resumeWithException(exc)
            }
        }
    )
}

/**
 * Writes the content of a [Flow] of ByteArrays to this [AsynchronousFileChannel]
 * starting at the specified [start] position.
 *
 * @param start The position in the file at which the writing starts.
 * @param content The [Flow] of ByteArrays to be written to the channel.
 *
 * Example usage:
 * ```
 * val channel: AsynchronousFileChannel = ...
 * val byteArrayFlow: Flow<ByteArray> = ...
 * channel.writeFlow(0, byteArrayFlow)
 * ```
 */
suspend fun AsynchronousFileChannel.writeFlow(
    start: Long,
    content: Flow<ByteArray>
) = content
    .runningFold(start to null as ByteArray?) { acc, item ->
        val (total, previous) = acc
        val previousSize = previous?.size ?: 0
        (total + previousSize) to item
    }
    .filterNot { (_, b) -> b == null }
    .collect { (position, item) -> coWrite(ByteBuffer.wrap(item), position) }

/**
 * Creates a [Flow] of ByteArrays by reading the content of this [AsynchronousFileChannel] using
 * the given [bufferSize] as the chunk size for each emitted ByteArray.
 *
 * @param bufferSize The size of the buffer to read data chunks into. Defaults to 1024.
 *
 * @return A [Flow] of ByteArrays representing chunks of content from this channel.
 *
 * Example usage:
 * ```
 * val channel: AsynchronousFileChannel = ...
 * val byteArrayFlow = channel.flow(1024)
 * byteArrayFlow.collect { bytes -> ... }
 * ```
 */
fun AsynchronousFileChannel.flow(
    bufferSize: Int = 1024
): Flow<ByteArray> =
    flow {
        var position = 0L

        while (position != -1L) {
            val bytes =
                coRead(bufferSize, position)
                    .array()
                    .filterNot { it.toInt() == 0 }
                    .toByteArray()

            if (bytes.isEmpty()) {
                position = -1
            } else {
                emit(bytes)
                position += bytes.size
            }
        }
    }
