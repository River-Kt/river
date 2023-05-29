@file:OptIn(ExperimentalCoroutinesApi::class)

package com.river.connector.rdbms.jdbc

import com.river.core.GroupStrategy
import com.river.core.GroupStrategy.*
import com.river.core.chunked
import com.river.core.mapAsync
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.invoke
import java.sql.PreparedStatement
import java.sql.ResultSet
import kotlin.reflect.full.primaryConstructor
import kotlin.time.Duration.Companion.milliseconds

typealias Row = Map<String, Any?>

/**
 * Executes a single SQL update using a JDBC connection.
 *
 * This function takes an SQL statement [sql] and an optional [prepare] function, which prepares
 * the statement before execution.
 *
 * @param sql The SQL statement to execute.
 * @param prepare A suspend function that prepares the statement before execution.
 * @return A [Flow] of the number of rows affected by the update.
 *
 * Example:
 * ```
 * val jdbc: Jdbc = ...
 * val sql = "UPDATE users SET active = 1 WHERE id = 42"
 *
 * jdbc.singleUpdate(sql)
 *     .collect { rowsAffected ->
 *         println("Rows affected: $rowsAffected")
 *     }
 * ```
 */
fun Jdbc.singleUpdate(
    sql: String,
    prepare: suspend PreparedStatement.() -> Unit = {}
): Flow<Int> = flow {
    connectionPool
        .use { IO { it.prepareStatement(sql).also { prepare(it) }.executeUpdate() } }
        .let { emit(it) }
}

/**
 * Executes a single SQL update using a JDBC connection for each item in the [upstream] flow.
 *
 * This function takes an SQL statement [sql], an [upstream] flow, [parallelism], and an optional
 * [prepare] function, which prepares the statement before execution.
 *
 * @param sql The SQL statement to execute.
 * @param upstream A [Flow] of items to process.
 * @param parallelism The level of parallelism for executing the updates.
 * @param prepare A suspend function that prepares the statement for each item before execution.
 * @return A [Flow] of the number of rows affected by the update for each item.
 *
 * Example:
 * ```
 * val jdbc: Jdbc = ...
 * val sql = "UPDATE users SET active = 1 WHERE id = ?"
 * val userIds = flowOf(1, 2, 3, 4, 5)
 *
 * jdbc.singleUpdate(sql, userIds, prepare = { id ->
 *     setInt(1, id)
 * }).collect { rowsAffected ->
 *     println("Rows affected: $rowsAffected")
 * }
 * ```
 */
fun <T> Jdbc.singleUpdate(
    sql: String,
    upstream: Flow<T>,
    parallelism: Int = 1,
    prepare: suspend PreparedStatement.(T) -> Unit = {}
): Flow<Int> =
    upstream
        .mapAsync(parallelism) { item ->
        connectionPool.use {
            IO {
                it.prepareStatement(sql)
                    .also { ps -> prepare(ps, item) }
                    .executeUpdate()
            }
        }
    }

/**
 * Executes a batch update for the given SQL statement using the provided [upstream] Flow as input.
 * The batch update is performed in chunks, as specified by the [groupStrategy] parameter, and can be executed in parallel
 * using the specified [parallelism] level.
 *
 * @param sql the SQL statement to be executed
 * @param upstream the Flow of input elements to be used in the batch update
 * @param parallelism the level of parallelism to be used in the batch update (default: 1)
 * @param groupStrategy the chunking strategy to be used for processing the input elements (default: TimeWindow(100, 250.milliseconds))
 * @param prepare a function that prepares the PreparedStatement using the current input element (default: {})
 * @return A [Flow] of the number of rows affected by the update for each batch.
 *
 * Example usage:
 *
 * ```
 *  val data = flowOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
 *  val updatedRows = Jdbc.batchUpdate("UPDATE my_table SET value = ? WHERE id = ?", data) { ps, value ->
 *      ps.setInt(1, value)
 *      ps.setInt(2, value)
 *  }
 *  updatedRows.collect { println("Updated $it rows.") }
 * ```
 */
fun <T> Jdbc.batchUpdate(
    sql: String,
    upstream: Flow<T>,
    parallelism: Int = 1,
    groupStrategy: GroupStrategy = TimeWindow(100, 250.milliseconds),
    prepare: suspend PreparedStatement.(T) -> Unit = {}
): Flow<Int> =
    upstream
        .chunked(groupStrategy)
        .mapAsync(parallelism) { chunk ->
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

/**
 * Executes a typed query for the given SQL statement using the provided [fetchSize] as a result set fetch size.
 * The resulting [Flow] emits objects of type [T], which must have a primary constructor and whose parameters are
 * matched with the columns of the result set.
 *
 * @param sql the SQL statement to be executed
 * @param fetchSize the result set fetch size to be used in the query (default: 100)
 * @return a Flow of objects of type [T], whose parameters are matched with the columns of the result set.
 *
 * Example usage:
 *
 * ```
 *  data class Person(val name: String, val age: Int)
 *
 *  val people = Jdbc.typedQuery<Person>("SELECT name, age FROM people WHERE age > 18")
 *  people.collect { println(it) }
 * ```
 */
inline fun <reified T : Any> Jdbc.typedQuery(
    sql: String,
    fetchSize: Int = 100,
): Flow<T> = typedQuery(sql, fetchSize) { }

/**
 * Executes a typed query for the given SQL statement using the provided [fetchSize] as a result set fetch size.
 * The resulting [Flow] emits objects of type [T], which must have a primary constructor and whose parameters are
 * matched with the columns of the result set.
 *
 * @param sql the SQL statement to be executed
 * @param fetchSize the result set fetch size to be used in the query (default: 100)
 * @param prepare a function that prepares the PreparedStatement for the query (default: {})
 * @return a Flow of objects of type [T], whose parameters are matched with the columns of the result set.
 *
 * Example usage:
 * ```
 *  data class Person(val name: String, val age: Int)
 *
 *  val people = Jdbc.typedQuery<Person>("SELECT name, age FROM people WHERE age > ?") {
 *      setInt(1, 18)
 *  }
 *
 *  people.collect { println(it) }
 * ```
 */
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

/**
 * Executes a query for the given SQL statement using the provided [fetchSize] as a result set fetch size.
 *
 * @param sql the SQL statement to be executed
 * @param fetchSize the result set fetch size to be used in the query (default: 100)
 * @param prepare a function that prepares the PreparedStatement for the query (default: {})
 * @return a Flow of Row objects, representing the result set.
 *
 * Example usage:
 * ```
 *  val result = Jdbc.query("SELECT * FROM my_table WHERE id = ?") {
 *      setInt(1, 10)
 *  }
 *  result.collect { println(it) }
 *```
 */
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
