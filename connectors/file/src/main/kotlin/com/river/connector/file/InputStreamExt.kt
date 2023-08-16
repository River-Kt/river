package com.river.connector.file

import com.river.core.ExperimentalRiverApi
import com.river.core.asByteArray
import com.river.core.poll
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream

/**
 * Converts the content of an [InputStream] into a [Flow] of ByteArrays.
 *
 * @param dispatcher The coroutine dispatcher to be used. Defaults to [Dispatchers.IO].
 *
 * @return A [Flow] of ByteArrays representing chunks of content from the input stream.
 *
 * Example usage:
 * ```
 * val inputStream: InputStream = ...
 * val byteArrayFlow = inputStream.asFlow()
 * byteArrayFlow.collect { bytes -> ... }
 * ```
 */
@ExperimentalRiverApi
fun InputStream.asFlow(
    dispatcher: CoroutineDispatcher = Dispatchers.IO
): Flow<ByteArray> =
    flow {
        use {
            poll(stopOnEmptyList = true) { readNBytes(8).toList() }
                .asByteArray()
                .collect { emit(it) }
        }
    }.flowOn(dispatcher)

/**
 * Converts a [Flow] of ByteArrays into an [InputStream].
 *
 * This function consumes the flow and writes its content into an InputStream, which allows you to
 * interface with APIs that expect an InputStream based on reactive data sources.
 *
 * @param bufferSize The size of the buffer to be used for the [PipedInputStream]. Defaults to 1024.
 * @param dispatcher The coroutine dispatcher to be used. Defaults to [Dispatchers.IO].
 *
 * @return An [InputStream] that provides the content from the flow.
 *
 * Example usage:
 * ```
 * val byteArrayFlow: Flow<ByteArray> = ...
 * val inputStream = byteArrayFlow.asInputStream()
 * // Use the inputStream, e.g., inputStream.read()
 * ```
 */
@ExperimentalRiverApi
suspend fun Flow<ByteArray>.asInputStream(
    bufferSize: Int = 1024,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
): InputStream = withContext(dispatcher) {
    val os = PipedOutputStream()
    val inputStream = PipedInputStream(bufferSize).also { it.connect(os) }

    onCompletion { os.close() }
        .onEach { os.write(it) }
        .flowOn(dispatcher)
        .launchIn(this)

    inputStream
}
