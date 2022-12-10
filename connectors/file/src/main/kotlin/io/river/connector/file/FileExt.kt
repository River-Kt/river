@file:OptIn(DelicateCoroutinesApi::class)

package io.river.connector.file

import io.river.core.collectAsync
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext
import java.io.*
import java.nio.file.OpenOption
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.coroutines.CoroutineContext
import kotlin.io.path.inputStream
import kotlin.io.path.writeBytes

internal val defaultContext by lazy { newSingleThreadContext("FileContext") }

fun InputStream.asFlow(
    context: CoroutineContext = defaultContext
) = channelFlow {
    buffered()
        .iterator()
        .forEach {
            try {
                send(it)
            } catch (e: Throwable) {
                close(e)
            }
        }
}.flowOn(context)

suspend fun Flow<ByteArray>.writeTo(
    context: CoroutineContext = defaultContext,
    outputStream: () -> OutputStream,
) = outputStream().let { os ->
    collect { withContext(context) { os.write(it) } }
    withContext(context) { os.close() }
}

fun Path.asFlow(
    vararg options: OpenOption
) = inputStream(*options).asFlow()

suspend fun Flow<ByteArray>.writeTo(
    path: Path,
    vararg options: OpenOption = arrayOf(
        StandardOpenOption.WRITE,
        StandardOpenOption.CREATE,
        StandardOpenOption.APPEND
    )
) = collect { path.writeBytes(it, *options) }

fun Flow<ByteArray>.zipAsFile(
    name: String
) = PipedOutputStream().let { os ->
    val zipChannel = Channel<ByteArray>()

    val pis = PipedInputStream().also { it.connect(os) }
    val zipOS = ZipOutputStream(os)

    val entry = ZipEntry(name)
    zipOS.putNextEntry(entry)

    zipChannel
        .consumeAsFlow()
        .collectAsync { zipOS.write(it) }

    onCompletion {
        zipChannel.close()
        zipOS.closeEntry()
        zipOS.close()
    }.collectAsync { zipChannel.send(it) }

    pis.asFlow()
}
