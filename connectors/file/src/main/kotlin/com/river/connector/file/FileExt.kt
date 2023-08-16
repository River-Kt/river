package com.river.connector.file

import com.river.core.ExperimentalRiverApi
import com.river.core.alsoTo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import java.nio.ByteBuffer
import java.nio.file.OpenOption
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/**
 * Reads the content of the specified [file] into a [Flow] of ByteArrays using the given [bufferSize].
 *
 * @param file The file path to be read.
 * @param bufferSize The size of the buffer to read data chunks into. Defaults to 1024.
 * @param options The options specifying how the file is opened. Defaults to [StandardOpenOption.READ].
 *
 * @return A [Flow] of ByteArrays representing chunks of content from the file.
 *
 * Example usage:
 * ```
 * val file: Path = ...
 * val byteArrayFlow = readFileAsFlow(file, 1024)
 * byteArrayFlow.collect { bytes -> ... }
 * ```
 */
fun readFileAsFlow(
    file: Path,
    bufferSize: Int = 1024,
    vararg options: OpenOption = arrayOf(
        StandardOpenOption.READ
    )
): Flow<ByteArray> = file.readAsFlow(bufferSize, *options)

/**
 * Writes the content of this [Flow] of ByteArrays to the specified [file].
 *
 * @param file The file path to write to.
 * @param options The options specifying how the file is opened. Defaults to [StandardOpenOption.WRITE] and [StandardOpenOption.CREATE].
 *
 * Example usage:
 * ```
 * val file: Path = ...
 * val byteArrayFlow: Flow<ByteArray> = ...
 * byteArrayFlow.writeTo(file)
 * ```
 */
suspend fun Flow<ByteArray>.writeTo(
    file: Path,
    vararg options: OpenOption = arrayOf(
        StandardOpenOption.WRITE,
        StandardOpenOption.CREATE
    )
) = file.writeFrom(this, *options)

/**
 * Appends the content of this [Flow] of ByteArrays to the specified [file].
 *
 * @param file The file path to append to.
 * @param options The options specifying how the file is opened. Defaults to [StandardOpenOption.WRITE] and [StandardOpenOption.CREATE].
 *
 * Example usage:
 * ```
 * val file: Path = ...
 * val byteArrayFlow: Flow<ByteArray> = ...
 * byteArrayFlow.appendTo(file)
 * ```
 */
suspend fun Flow<ByteArray>.appendTo(
    file: Path,
    vararg options: OpenOption = arrayOf(
        StandardOpenOption.WRITE,
        StandardOpenOption.CREATE
    )
) = file.appendFrom(this, *options)

/**
 * Transforms the content of this [Flow] of type [T] using the given [mapper] to a [Flow] of ByteArrays,
 * then writes the ByteArrays to the specified [file] while also returning the original content.
 *
 * @param file The file path to write to.
 * @param options The options specifying how the file is opened. Defaults to [StandardOpenOption.WRITE] and [StandardOpenOption.CREATE].
 * @param mapper A function to transform items of type [T] to [ByteArray].
 *
 * @return A [Flow] of type [T] representing the original content.
 *
 * Example usage:
 * ```
 * val file: Path = ...
 * val stringFlow: Flow<String> = ...
 * stringFlow
 *     .alsoWriteTo(file) { string -> string.toByteArray() }
 *     .collect { originalString -> ... }
 * ```
 */
@ExperimentalRiverApi
suspend fun <T> Flow<T>.alsoWriteTo(
    file: Path,
    vararg options: OpenOption = arrayOf(
        StandardOpenOption.WRITE,
        StandardOpenOption.CREATE
    ),
    mapper: suspend (T) -> ByteArray
): Flow<T> =
    alsoTo {
        val channel = file.asyncChannel(*options)
        var location = 0L

        map(mapper)
            .onEach { channel.coWrite(ByteBuffer.wrap(it), location) }
            .onEach { location += it.size }
    }.map { it.first }

/**
 * Transforms the content of this [Flow] of type [T] using the given [mapper] to a [Flow] of ByteArrays,
 * then appends the ByteArrays to the specified [file] while also returning the original content.
 *
 * @param file The file path to append to.
 * @param options The options specifying how the file is opened. Defaults to [StandardOpenOption.WRITE] and [StandardOpenOption.CREATE].
 * @param mapper A function to transform items of type [T] to [ByteArray].
 *
 * @return A [Flow] of type [T] representing the original content.
 *
 * Example usage:
 * ```
 * val file: Path = ...
 * val stringFlow: Flow<String> = ...
 * stringFlow
 *     .alsoAppendTo(file) { string -> string.toByteArray() }
 *     .collect { originalString -> ... }
 * ```
 */
@ExperimentalRiverApi
suspend fun <T> Flow<T>.alsoAppendTo(
    file: Path,
    vararg options: OpenOption = arrayOf(
        StandardOpenOption.WRITE,
        StandardOpenOption.CREATE
    ),
    mapper: suspend (T) -> ByteArray
): Flow<T> =
    alsoTo {
        val channel = file.asyncChannel(*options)
        var location = channel.size()

        map(mapper)
            .onEach { channel.coWrite(ByteBuffer.wrap(it), location) }
            .onEach { location += it.size }
    }.map { it.first }
