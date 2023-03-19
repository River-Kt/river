@file:OptIn(ExperimentalCoroutinesApi::class)

package io.river.connector.rdbms.jdbc

import io.river.core.ChunkStrategy
import io.river.core.ChunkStrategy.*
import io.river.core.chunked
import io.river.core.mapParallel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.invoke
import java.sql.PreparedStatement
import java.sql.ResultSet
import kotlin.reflect.full.primaryConstructor
import kotlin.time.Duration.Companion.milliseconds

typealias Row = Map<String, Any?>

fun Jdbc.singleUpdate(
    sql: String,
    prepare: suspend PreparedStatement.() -> Unit = {}
): Flow<Int> = flow {
    connectionPool
        .use { IO { it.prepareStatement(sql).also { prepare(it) }.executeUpdate() } }
        .let { emit(it) }
}

fun <T> Jdbc.singleUpdate(
    sql: String,
    upstream: Flow<T>,
    parallelism: Int = 1,
    prepare: suspend PreparedStatement.(T) -> Unit = {}
): Flow<Int> =
    upstream
        .mapParallel(parallelism) { item ->
        connectionPool.use {
            IO {
                it.prepareStatement(sql)
                    .also { ps -> prepare(ps, item) }
                    .executeUpdate()
            }
        }
    }

fun <T> Jdbc.batchUpdate(
    sql: String,
    upstream: Flow<T>,
    parallelism: Int = 1,
    chunkStrategy: ChunkStrategy = TimeWindow(100, 250.milliseconds),
    prepare: suspend PreparedStatement.(T) -> Unit = {}
): Flow<Int> =
    upstream
        .chunked(chunkStrategy)
        .mapParallel(parallelism) { chunk ->
            connectionPool.use {
                IO {
                    logger.debug("Running $sql with ${chunk.size} elements.")

                    it.prepareStatement(sql)
                        .also { ps -> chunk.forEach { prepare(ps, it); ps.addBatch() } }
                        .executeBatch()
                        .size
                        .also { logger.debug("Finished running batch update: $it rows were updated.") }
                }
            }
        }

inline fun <reified T : Any> Jdbc.typedQuery(
    sql: String,
    fetchSize: Int = 100,
): Flow<T> = typedQuery(sql, fetchSize) { }

inline fun <reified T : Any> Jdbc.typedQuery(
    sql: String,
    fetchSize: Int = 100,
    crossinline prepare: suspend PreparedStatement.() -> Unit,
): Flow<T> =
    query(sql, fetchSize) { prepare() }
        .map { row ->
            val constructor = checkNotNull(T::class.primaryConstructor) {
                "Class ${T::class.simpleName} does not have a primary constructor"
            }

            constructor.callBy(
                constructor.parameters.associateWith { row[it.name] }
            )
        }

fun Jdbc.query(
    sql: String,
    fetchSize: Int = 100,
    prepare: suspend PreparedStatement.() -> Unit = {}
): Flow<Row> = flow {
    connectionPool.use { connection ->
        val resultSet: ResultSet = IO {
            val statement = connection.prepareStatement(sql).also { prepare(it) }
            statement.fetchSize = fetchSize
            statement.executeQuery()
        }

        val metaData = resultSet.metaData
        val columns = metaData.columnCount

        while (IO { resultSet.next() }) {
            (1..columns)
                .associate { metaData.getColumnName(it) to resultSet.getObject(it) }
                .let { emit(it) }
        }
    }
}
