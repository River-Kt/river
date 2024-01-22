package com.river.connector.file

import com.river.connector.file.model.ContentfulZipEntry
import com.river.core.ExperimentalRiverApi
import com.river.core.asByteArray
import com.river.core.launch
import com.river.core.poll
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import java.io.InputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Compresses the given [Flow] of ByteArrays into a ZIP file containing a single entry with the provided [entryName].
 *
 * @param entryName The name of the ZIP entry.
 * @param dispatcher The coroutine dispatcher to be used. Defaults to [Dispatchers.IO].
 *
 * @return A [Flow] of ByteArrays representing the compressed ZIP content.
 *
 * Example usage:
 * ```
 * val byteArrayFlow: Flow<ByteArray> = ...
 * val zippedFlow = byteArrayFlow.zipFile("example.txt")
 * zippedFlow.collect { bytes -> ... }
 * ```
 */
@ExperimentalRiverApi
fun Flow<ByteArray>.zipFile(
    entryName: String,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
): Flow<ByteArray> = channelFlow {
    PipedOutputStream().let { os ->
        val zipChannel = Channel<ByteArray>()

        val pis = PipedInputStream().also { it.connect(os) }
        val zipOS = ZipOutputStream(os)

        val entry = ZipEntry(entryName)
        zipOS.putNextEntry(entry)

        zipChannel
            .consumeAsFlow()
            .flowOn(dispatcher)
            .onEach { zipOS.write(it) }
            .launch()

        flowOn(dispatcher)
            .onCompletion {
                zipChannel.close()
                zipOS.closeEntry()
                zipOS.close()
            }
            .onEach { zipChannel.send(it) }
            .launch()

        pis.asFlow().collect { send(it) }
    }
}.flowOn(dispatcher)

/**
 * Decompresses the content from a given [Flow] of ByteArrays representing a ZIP file.
 *
 * @param dispatcher The coroutine dispatcher to be used. Defaults to [Dispatchers.IO].
 *
 * @return A [Flow] of [ContentfulZipEntry] representing each entry in the ZIP file along with its data.
 *
 * Example usage:
 * ```
 * val zippedFlow: Flow<ByteArray> = ...
 * val unzippedEntriesFlow = zippedFlow.unzipFile()
 * unzippedEntriesFlow.collect { entry -> ... }
 * ```
 */
@ExperimentalRiverApi
fun Flow<ByteArray>.unzipFile(
    dispatcher: CoroutineDispatcher = Dispatchers.IO
): Flow<ContentfulZipEntry> = channelFlow {
    coroutineScope {
        val os = PipedOutputStream()
        val pis = PipedInputStream().also { it.connect(os) }

        val zipChannel = Channel<ByteArray>()
        val zipIS = ZipInputStream(pis)

        val readJob = launch {
            while (isActive) {
                zipIS.nextEntry?.also { entry ->
                    send(ContentfulZipEntry(entry, zipIS.readBytes()))
                }
            }
        }

        val writeJob =
            zipChannel
                .consumeAsFlow()
                .flowOn(dispatcher)
                .onEach { os.write(it) }
                .launchIn(this)

        flowOn(dispatcher).collect { zipChannel.send(it) }

        zipChannel.close()
        writeJob.cancelAndJoin()
        readJob.cancelAndJoin()
        os.close()
        zipIS.close()
        pis.close()
    }
}.flowOn(dispatcher)
