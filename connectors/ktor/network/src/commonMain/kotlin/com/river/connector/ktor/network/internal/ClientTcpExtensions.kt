package com.river.connector.ktor.network.internal

import com.river.connector.ktor.network.awaitClosed
import com.river.connector.ktor.network.flowOfBytes
import com.river.connector.ktor.network.use
import com.river.core.launchCollect
import com.river.core.withPromise
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

internal interface ClientTcpExtensions {
    context(CoroutineScope)
    suspend fun connectAsync(
        hostname: String,
        port: Int,
        builder: SocketOptions.TCPClientSocketOptions.() -> Unit = {},
        dispatcher: CoroutineContext = EmptyCoroutineContext,
        handle: suspend Connection.() -> Unit
    ): Connection =
        withPromise { promise ->
            connection(hostname, port, builder, dispatcher)
                .launchCollect {
                    promise.complete(it)
                    handle(it)
                }
        }

    suspend fun connect(
        hostname: String,
        port: Int,
        builder: SocketOptions.TCPClientSocketOptions.() -> Unit = {},
        dispatcher: CoroutineContext = EmptyCoroutineContext,
        handle: suspend Connection.() -> Unit
    ): Unit =
        connection(hostname, port, builder, dispatcher)
            .collect { handle(it) }

    fun connectAsFlow(
        hostname: String,
        port: Int,
        builder: SocketOptions.TCPClientSocketOptions.() -> Unit = {},
        dispatcher: CoroutineContext = EmptyCoroutineContext,
    ): Flow<Byte> =
        flow {
            val connection = connection(hostname, port, builder, dispatcher)

            connection
                .collect { conn ->
                    conn.use { emitAll(it.flowOfBytes()) }
                }
        }
}

internal fun connection(
    hostname: String,
    port: Int,
    builder: SocketOptions.TCPClientSocketOptions.() -> Unit = {},
    dispatcher: CoroutineContext = EmptyCoroutineContext
): Flow<Connection> =
    flow {
        SelectorManager(dispatcher)
            .use { selectorManager ->
                aSocket(selectorManager)
                    .tcp()
                    .connect(hostname, port) { builder(this) }
                    .connection()
                    .also { emit(it) }
                    .awaitClosed()
            }
    }
