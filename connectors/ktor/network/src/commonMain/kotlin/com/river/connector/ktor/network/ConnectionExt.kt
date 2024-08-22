package com.river.connector.ktor.network

import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.yield

suspend inline fun Connection.awaitBytes(size: Int = 8): ByteArray =
    input
        .readPacket(size)
        .readBytes(size)

suspend inline fun Connection.awaitLine(): String? =
    input.readUTF8Line()

suspend inline fun Connection.awaitLine(ifEmpty: () -> String): String =
    awaitLine() ?: ifEmpty()

suspend fun Connection.whileActive(operation: suspend Connection.() -> Unit) {
    while (isConnectionActive) {
        operation()
        yield()
    }
}

inline fun Connection.flush(f: Connection.() -> Unit) {
    f(this)
    output.flush()
}

suspend inline fun Connection.writeLine(
    data: CharSequence = "",
    flush: Boolean = false
) = write("$data\n", flush)

suspend inline fun Connection.write(
    data: CharSequence,
    flush: Boolean = false
) {
    output.writePacketSuspend { append(data) }
    if (flush) output.flush()
}

suspend inline fun Connection.write(
    data: ByteArray,
    flush: Boolean = false
) {
    output.writePacket(data.packet)
    if (flush) output.flush()
}

inline fun Connection.flowOfByteArray(size: Int = 8) =
    flowOf { awaitBytes(size) }

inline fun Connection.flowOfBytes(): Flow<Byte> =
    flowOf { input.readByte() }

inline fun Connection.flowOfLines() =
    flowOf { awaitLine() }
        .filterNotNull()

inline fun <T> Connection.flowOf(crossinline next: suspend Connection.() -> T) =
    flow {
        var shouldContinue: Boolean = !isClosedForRead

        while (shouldContinue) {
            try {
                emit(next())
                shouldContinue = !isClosedForRead
            } catch (e: ClosedReceiveChannelException) {
                shouldContinue = false
            }
        }
    }

val Connection.isConnectionActive: Boolean
    get() = !isClosedForRead && !isClosedForWrite

val Connection.isClosedForRead: Boolean
    get() = socket.isClosed || input.isClosedForRead

val Connection.isClosedForWrite: Boolean
    get() = socket.isClosed || output.isClosedForWrite

val ByteArray.packet: ByteReadPacket
    get() = ByteReadPacket(this)

suspend fun Connection.awaitClosed() =
    socket.awaitClosed()

fun Connection.close() {
    output.close()
    input.cancel()
    socket.close()
}

inline fun <T> Connection.use(block: (Connection) -> T) =
    try {
        block(this)
    } finally {
        close()
    }
