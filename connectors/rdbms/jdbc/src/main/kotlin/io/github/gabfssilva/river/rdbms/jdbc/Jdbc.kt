@file:OptIn(ExperimentalCoroutinesApi::class)

package io.github.gabfssilva.river.rdbms.jdbc

import io.github.gabfssilva.river.util.pool.ObjectPool
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.DriverManager

class Jdbc(
    connectionPoolSize: Int = 10,
    private val connectionFactory: () -> Connection,
) {
    internal val logger = LoggerFactory.getLogger(this.javaClass)

    internal val IO: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(connectionPoolSize)

    internal val connectionPool = ObjectPool.sized(
        maxSize = connectionPoolSize,
        onClose = { IO { it.close() } },
        factory = { IO { connectionFactory() } }
    )

    suspend fun close(): Unit {
        connectionPool.close()
        IO.cancel()
    }

    companion object {
        operator fun invoke(
            url: String,
            connectionPoolSize: Int = 10,
            credentials: Pair<String, String>? = null,
        ) = Jdbc(connectionPoolSize) {
            credentials
                ?.let { DriverManager.getConnection(url, it.first, it.second) }
                ?: DriverManager.getConnection(url)
        }
    }
}
