@file:OptIn(ExperimentalCoroutinesApi::class)

package io.github.gabfssilva.river.rdbms.jdbc

import io.github.gabfssilva.river.core.*
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
    connectionPool.use {
        IO {
            it.prepareStatement(sql)
                .also { ps -> prepare(ps) }
                .executeUpdate()
                .let { emit(it) }
        }
    }
}

context(Flow<T>)
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

fun <T> Jdbc.singleUpdate(
    sql: String,
    upstream: Flow<T>,
    parallelism: Int = 1,
    prepare: suspend PreparedStatement.(T) -> Unit = {}
): Flow<Int> =
    with(upstream) {
        singleUpdate(sql, upstream, parallelism, prepare)
    }

fun <T> Jdbc.batchUpdate(
    sql: String,
    upstream: Flow<T>,
    parallelism: Int = 1,
    chunkStrategy: ChunkStrategy = ChunkStrategy.TimeWindow(100, 250.milliseconds),
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
): Flow<T> = typedQuery(sql) { }

inline fun <reified T : Any> Jdbc.typedQuery(
    sql: String,
    crossinline prepare: suspend PreparedStatement.() -> Unit,
): Flow<T> =
    query(sql) { prepare() }
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
    prepare: suspend PreparedStatement.() -> Unit = {}
): Flow<Row> = flow {
    connectionPool.use { connection ->
        val resultSet: ResultSet = IO {
            val statement = connection.prepareStatement(sql).also { prepare(it) }
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
