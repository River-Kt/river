@file:OptIn(ExperimentalCoroutinesApi::class)

package io.github.gabfssilva.river.rdbms.jdbc

import io.github.gabfssilva.river.core.*
import io.github.gabfssilva.river.util.pool.ObjectPool
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.invoke
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import kotlin.time.Duration.Companion.milliseconds

class Jdbc(
    connectionPoolSize: Int = 10,
    private val connectionFactory: () -> Connection,
) {
    private val IO: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(connectionPoolSize)

    private val connectionPool = ObjectPool.sized(
        maxSize = connectionPoolSize,
        onClose = { IO { it.close() } },
        factory = { IO { connectionFactory() } }
    )

    fun singleUpdate(
        sql: String,
        f: suspend PreparedStatement.() -> Unit = {}
    ): Flow<Int> = flow {
        connectionPool.use {
            IO {
                it.prepareStatement(sql)
                    .also { ps -> f(ps) }
                    .executeUpdate()
                    .let { emit(it) }
            }
        }
    }

    context(Flow<T>)
    fun <T> singleUpdate(
        sql: String,
        upstream: Flow<T>,
        paralellism: Int = 1,
        f: suspend PreparedStatement.(T) -> Unit = {}
    ): Flow<Int> =
        upstream
            .mapParallel(paralellism) { item ->
                connectionPool.use {
                    IO {
                        it.prepareStatement(sql)
                            .also { ps -> f(ps, item) }
                            .executeUpdate()
                    }
                }
            }

    fun <T> singleUpdate(
        sql: String,
        upstream: Flow<T>,
        paralellism: Int = 1,
        f: suspend PreparedStatement.(T) -> Unit = {}
    ): Flow<Int> =
        with(upstream) {
            singleUpdate(sql, upstream, paralellism, f)
        }

    fun <T> batchUpdate(
        sql: String,
        upstream: Flow<T>,
        paralellism: Int = 1,
        chunkStrategy: ChunkStrategy = ChunkStrategy.TimeWindow(100, 250.milliseconds),
        f: suspend PreparedStatement.(T) -> Unit = {}
    ): Flow<Int> =
        upstream
            .chunked(chunkStrategy)
            .mapParallel(paralellism) { chunk ->
                connectionPool.use {
                    IO {
                        it.prepareStatement(sql)
                            .also { ps -> chunk.forEach { f(ps, it); ps.addBatch() } }
                            .executeBatch()
                            .size
                    }
                }
            }

    fun query(
        sql: String,
        f: suspend PreparedStatement.() -> Unit = {}
    ): Flow<Map<String, Any>> =
        flow {
            val connection = connectionPool.borrow()

            val resultSet: ResultSet = IO {
                val statement = connection.instance.prepareStatement(sql).also { f(it) }
                statement.executeQuery()
            }

            val metaData = resultSet.metaData
            val columns = metaData.columnCount

            suspend fun coNext() =
                IO { resultSet.next() }

            while (coNext()) {
                (1..columns)
                    .associate { metaData.getColumnName(it) to resultSet.getObject(it) }
                    .let { emit(it) }
            }

            connectionPool.release(connection)
        }

    suspend fun close() = IO { connectionPool.close() }

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
