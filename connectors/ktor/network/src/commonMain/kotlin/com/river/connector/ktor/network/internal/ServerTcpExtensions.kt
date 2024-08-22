package com.river.connector.ktor.network.internal

import com.river.connector.ktor.network.use
import com.river.core.collectAsync
import com.river.core.poll
import com.river.core.withPromise
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

internal interface ServerTcpExtensions {
    context(CoroutineScope)
    suspend fun bindAsync(
        hostname: String,
        port: Int,
        maxConnections: Int = 10,
        builder: SocketOptions.AcceptorOptions.() -> Unit = {},
        dispatcher: CoroutineContext = EmptyCoroutineContext,
        handle: suspend Connection.() -> Unit
    ): ServerSocket =
        withPromise { promise ->
            bindAsFlow(hostname, port, maxConnections, builder, dispatcher)
                .collect { (socket, connections) ->
                    promise.complete(socket)

                    coroutineScope {
                        val bindingJob = launch {
                            connections
                                .collectAsync(maxConnections) { connection -> connection.use { handle(it) } }
                        }

                        socket.awaitClosed()
                        bindingJob.cancelAndJoin()
                    }
                }
        }

    suspend fun bind(
        hostname: String,
        port: Int,
        maxConnections: Int = 10,
        builder: SocketOptions.AcceptorOptions.() -> Unit = {},
        dispatcher: CoroutineContext = EmptyCoroutineContext,
        handle: suspend Connection.() -> Unit
    ): Unit =
        bindAsFlow(hostname, port, maxConnections, builder, dispatcher)
            .collect { (_, connections) ->
                connections.collectAsync(maxConnections) { connection ->
                    connection.use { handle(it) }
                }
            }

    fun bindAsFlow(
        hostname: String,
        port: Int,
        maxConnections: Int = 10,
        builder: SocketOptions.AcceptorOptions.() -> Unit = {},
        dispatcher: CoroutineContext = EmptyCoroutineContext,
    ): Flow<Pair<ServerSocket, Flow<Connection>>> =
        flow {
            SelectorManager(dispatcher)
                .use { selectorManager ->
                    aSocket(selectorManager)
                        .tcp()
                        .bind(hostname, port, builder)
                        .use { serverSocket ->
                            emit(serverSocket to serverSocket.acceptAsFlow())
                            serverSocket.awaitClosed()
                        }
                }
        }

    fun ServerSocket.acceptAsFlow(): Flow<Connection> =
        poll(stopOnEmptyList = true) {
            if (isClosed) emptyList()
            else runCatching { listOf(accept().connection()) }.getOrElse { emptyList() }
        }
}
