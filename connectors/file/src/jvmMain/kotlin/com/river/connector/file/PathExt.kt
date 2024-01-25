package com.river.connector.file

import kotlinx.coroutines.flow.Flow
import java.nio.channels.AsynchronousFileChannel
import java.nio.file.OpenOption
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/**
 * Reads the content of the file at this [Path] into a [Flow] of ByteArrays. Each emitted ByteArray
 * represents a chunk of the file content based on the specified [bufferSize].
 *
 * @param bufferSize The size of the buffer to read data chunks into. Defaults to 1024.
 * @param options The options specifying how the file is opened. Defaults to [StandardOpenOption.READ].
 *
 * @return A [Flow] of ByteArrays representing chunks of content from the file.
 *
 * Example usage:
 * ```
 * val filePath: Path = ...
 * val byteArrayFlow = filePath.readAsFlow(1024)
 * byteArrayFlow.collect { bytes -> ... }
 * ```
 */
fun Path.readAsFlow(
    bufferSize: Int = 1024,
    vararg options: OpenOption = arrayOf(
        StandardOpenOption.READ
    ),
) = asyncChannel(*options).flow(bufferSize)

/**
 * Appends the content from a given [Flow] of ByteArrays to the file at this [Path].
 *
 * @param content The [Flow] of ByteArrays to be written to the file.
 * @param options The options specifying how the file is opened. Defaults to [StandardOpenOption.WRITE] and [StandardOpenOption.CREATE].
 *
 * Example usage:
 * ```
 * val filePath: Path = ...
 * val byteArrayFlow: Flow<ByteArray> = ...
 * filePath.appendFrom(byteArrayFlow)
 * ```
 */
suspend fun Path.appendFrom(
    content: Flow<ByteArray>,
    vararg options: OpenOption = arrayOf(
        StandardOpenOption.WRITE,
        StandardOpenOption.CREATE,
    )
) = asyncChannel(*options).let { channel -> channel.writeFlow(channel.size(), content) }

/**
 * Writes the content from a given [Flow] of ByteArrays to the file at this [Path], starting at the beginning.
 *
 * @param content The [Flow] of ByteArrays to be written to the file.
 * @param options The options specifying how the file is opened. Defaults to [StandardOpenOption.WRITE] and [StandardOpenOption.CREATE].
 *
 * Example usage:
 * ```
 * val filePath: Path = ...
 * val byteArrayFlow: Flow<ByteArray> = ...
 * filePath.writeFrom(byteArrayFlow)
 * ```
 */
suspend fun Path.writeFrom(
    content: Flow<ByteArray>,
    vararg options: OpenOption = arrayOf(
        StandardOpenOption.WRITE,
        StandardOpenOption.CREATE,
    )
) = asyncChannel(*options).writeFlow(0, content)

/**
 * Opens the file at this [Path] with the specified [options] and returns an [AsynchronousFileChannel] to it.
 *
 * @param options The options specifying how the file is opened.
 *
 * @return The [AsynchronousFileChannel] associated with the file.
 *
 * Example usage:
 * ```
 * val filePath: Path = ...
 * val channel = filePath.asyncChannel(StandardOpenOption.READ)
 * ```
 */
fun Path.asyncChannel(vararg options: OpenOption): AsynchronousFileChannel =
    AsynchronousFileChannel.open(this, *options)
