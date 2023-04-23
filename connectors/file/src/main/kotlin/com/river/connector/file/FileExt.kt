@file:OptIn(DelicateCoroutinesApi::class)

package com.river.connector.file

import com.river.core.collectAsync
import com.river.core.unfold
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.nio.file.OpenOption
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.inputStream
import kotlin.io.path.writeBytes

suspend fun Flow<ByteArray>.writeTo(
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    outputStream: () -> OutputStream,
) = outputStream().let { os ->
    withContext(dispatcher) {
        os.use { collect { os.write(it) } }
    }
}

fun Path.asFlow(
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    vararg options: OpenOption
) = inputStream(*options).asFlow(dispatcher)

suspend fun Flow<ByteArray>.writeTo(
    path: Path,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    vararg options: OpenOption = arrayOf(
        StandardOpenOption.WRITE,
        StandardOpenOption.CREATE,
        StandardOpenOption.APPEND
    )
) = flowOn(dispatcher).collect { path.writeBytes(it, *options) }

fun Flow<ByteArray>.zipAsFile(
    name: String,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
) = flow {
    PipedOutputStream().let { os ->
        val zipChannel = Channel<ByteArray>()

        val pis = PipedInputStream().also { it.connect(os) }
        val zipOS = ZipOutputStream(os)

        val entry = ZipEntry(name)
        zipOS.putNextEntry(entry)

        zipChannel
            .consumeAsFlow()
            .flowOn(dispatcher)
            .collectAsync { zipOS.write(it) }

        flowOn(dispatcher)
            .onCompletion {
                zipChannel.close()
                zipOS.closeEntry()
                zipOS.close()
            }
            .collectAsync { zipChannel.send(it) }

        emitAll(pis.asFlow())
    }
}.flowOn(dispatcher)

suspend fun Flow<ByteArray>.asInputStream(
    bufferSize: Int = 1024,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
): InputStream = withContext(dispatcher) {
    val os = PipedOutputStream()
    val inputStream = PipedInputStream(bufferSize).also { it.connect(os) }

    onCompletion { os.close() }
        .collectAsync(this) { os.write(it) }

    inputStream
}

fun InputStream.asFlow(
    dispatcher: CoroutineDispatcher = Dispatchers.IO
): Flow<Byte> =
    flow {
        use {
            unfold(true) { readNBytes(8).toList() }
                .collect { emit(it) }
        }
    }.flowOn(dispatcher)
