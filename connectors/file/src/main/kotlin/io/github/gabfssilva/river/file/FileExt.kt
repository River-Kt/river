@file:OptIn(DelicateCoroutinesApi::class)

package io.github.gabfssilva.river.file

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.OpenOption
import java.nio.file.Path
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
    vararg options: OpenOption
) = collect { path.writeBytes(it, *options) }
