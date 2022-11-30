@file:OptIn(ExperimentalCoroutinesApi::class)

package io.github.gabfssilva.river.rdbms.jdbc

import io.github.gabfssilva.river.core.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.invoke
import java.sql.PreparedStatement
import java.sql.ResultSet
import kotlin.reflect.KClass
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
    paralellism: Int = 1,
    prepare: suspend PreparedStatement.(T) -> Unit = {}
): Flow<Int> =
    upstream
        .mapParallel(paralellism) { item ->
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
    paralellism: Int = 1,
    prepare: suspend PreparedStatement.(T) -> Unit = {}
): Flow<Int> =
    with(upstream) {
        singleUpdate(sql, upstream, paralellism, prepare)
    }

fun <T> Jdbc.batchUpdate(
    sql: String,
    upstream: Flow<T>,
    paralellism: Int = 1,
    chunkStrategy: ChunkStrategy = ChunkStrategy.TimeWindow(100, 250.milliseconds),
    prepare: suspend PreparedStatement.(T) -> Unit = {}
): Flow<Int> =
    upstream
        .chunked(chunkStrategy)
        .mapParallel(paralellism) { chunk ->
            connectionPool.use {
                IO {
                    it.prepareStatement(sql)
                        .also { ps -> chunk.forEach { prepare(ps, it); ps.addBatch() } }
                        .executeBatch()
                        .size
                }
            }
        }

inline fun <reified T : Any> Row.default(): T {
    val clazz: KClass<T> = T::class
    val constructor = checkNotNull(clazz.primaryConstructor) {
        "Class ${clazz.simpleName} does not have a primary constructor"
    }
    return constructor.callBy(constructor.parameters.associateWith { get(it.name) })
}

inline fun <reified T : Any> Jdbc.typedQuery(
    sql: String,
): Flow<T> = typedQuery(sql) { }

inline fun <reified T : Any> Jdbc.typedQuery(
    sql: String,
    crossinline prepare: suspend PreparedStatement.() -> Unit,
): Flow<T> =
    query(sql) { prepare() }
        .map { it.default() }

fun Jdbc.query(
    sql: String,
    prepare: suspend PreparedStatement.() -> Unit = {}
): Flow<Row> =
    flow {
        val connection = connectionPool.borrow()

        val resultSet: ResultSet = IO {
            val statement = connection.instance.prepareStatement(sql).also { prepare(it) }
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
