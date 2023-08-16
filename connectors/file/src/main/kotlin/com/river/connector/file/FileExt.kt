package com.river.connector.file

import com.river.core.ExperimentalRiverApi
import com.river.core.asByteArray
import com.river.core.poll
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import java.io.InputStream
import java.io.OutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.nio.file.OpenOption
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.io.path.inputStream
import kotlin.io.path.writeBytes

@ExperimentalRiverApi
suspend fun Flow<ByteArray>.writeTo(
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    outputStream: () -> OutputStream,
) = outputStream().let { os ->
    withContext(dispatcher) {
        os.use { collect { os.write(it) } }
    }
}

@ExperimentalRiverApi
fun Path.asFlow(
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    vararg options: OpenOption
) = inputStream(*options).asFlow(dispatcher)

@ExperimentalRiverApi
suspend fun Flow<ByteArray>.writeTo(
    path: Path,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    vararg options: OpenOption = arrayOf(
        StandardOpenOption.WRITE,
        StandardOpenOption.CREATE,
        StandardOpenOption.APPEND
    )
) = flowOn(dispatcher).collect { path.writeBytes(it, *options) }

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
            .onEach { zipOS.write(it)  }
            .launchIn(this)

        flowOn(dispatcher)
            .onCompletion {
                zipChannel.close()
                zipOS.closeEntry()
                zipOS.close()
            }
            .onEach { zipChannel.send(it) }
            .launchIn(this)

        pis.asFlow().collect { send(it) }
    }
}.flowOn(dispatcher)

@ExperimentalRiverApi
suspend fun Flow<ByteArray>.asInputStream(
    bufferSize: Int = 1024,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
): InputStream = withContext(dispatcher) {
    val os = PipedOutputStream()
    val inputStream = PipedInputStream(bufferSize).also { it.connect(os) }

    onCompletion { os.close() }
        .onEach { os.write(it) }
        .launchIn(this)

    inputStream
}

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

@ExperimentalRiverApi
class ContentfulZipEntry(entry: ZipEntry, val data: ByteArray) : ZipEntry(entry)

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
