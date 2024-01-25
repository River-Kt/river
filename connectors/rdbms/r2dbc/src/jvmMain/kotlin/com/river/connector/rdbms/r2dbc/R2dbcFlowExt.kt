package com.river.connector.rdbms.r2dbc

import com.river.connector.rdbms.r2dbc.model.Returning
import com.river.core.*
import io.r2dbc.spi.Connection
import io.r2dbc.spi.Result
import io.r2dbc.spi.Row
import io.r2dbc.spi.Statement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import kotlin.time.Duration.Companion.milliseconds


typealias ResultRow = Map<String, Any?>

/**
 * Executes an SQL query and retrieves a Flow of ResultRows representing the returned data.
 *
 * @param sql The SQL query string to be executed.
 * @param prepare An optional lambda for preparing the statement, which allows you to configure
 * the statement further before executing it (e.g., bind parameters). Defaults to an empty lambda.
 *
 * @return A Flow of ResultRows with the data returned by the executed query.
 *
 * Example usage:
 *
 * ```
 * val connection: Connection = // Obtain a connection from your R2DBC connection factory
 * val sql = "SELECT * FROM users WHERE age > ?"
 * val age = 18
 *
 * connection.query(sql) { bind(0, age) }
 *     .collect { row ->
 *         println("User: ${row["name"]}, Age: ${row["age"]}")
 *     }
 * ```
 */
fun Connection.query(
    sql: String,
    prepare: Statement.() -> Unit = {}
): Flow<ResultRow> =
    flow {
        val flow =
            createStatement(sql)
                .also(prepare)
                .execute()
                .asFlow()
                .flatMapFlow {
                    it.map { row, rowMetadata ->
                        rowMetadata.columnMetadatas.associate { metadata ->
                            val columnName = metadata.name
                            columnName to row.get(columnName)
                        }
                    }.asFlow()
                }

        emitAll(flow)
    }

/**
 * Executes a single SQL update statement and returns the number of rows affected as a Flow<Long>.
 *
 * @param sql the SQL statement to execute
 *
 * @return a Flow<Long> that emits the number of rows affected by the SQL statement
 *
 * Example usage:
 *
 * ```
 * val connection: Connection = // Obtain a connection from your R2DBC connection factory
 *  val updateSql = "UPDATE users SET email='newemail@example.com' WHERE id=1"
 *  val numAffectedRows = connection.singleUpdate(updateSql).single()
 *  println("Updated $numAffectedRows rows")
 * ```
 */
fun Connection.singleUpdate(
    sql: String
): Flow<Long> =
    createStatement(sql)
        .execute()
        .asFlow()
        .flatMapFlow { it.rowsUpdated.asFlow() }

/**
 * Executes an SQL update statement for each item in the provided upstream Flow.
 *
 * @param T The type of the items emitted by the upstream Flow.
 * @param sql The SQL update statement to be executed.
 * @param upstream A Flow of items to be processed, where each item will be used to prepare an SQL statement.
 * @param concurrency An optional parameter to define the level of concurrency when processing items. Defaults to 1.
 * @param prepare An optional lambda for preparing the statement with an item from the upstream Flow,
 * which allows you to configure the statement further before executing it (e.g., bind parameters).
 * Defaults to an empty lambda.
 *
 * @return A Flow of Long values representing the number of rows updated for each executed statement.
 *
 * Example usage:
 *
 * ```
 * val connection: Connection = // Obtain a connection from your R2DBC connection factory
 * val sql = "UPDATE users SET name = ? WHERE id = ?"
 * val users = flowOf(1 to "Alice", 2 to "Bob")
 *
 * connection
 *     .singleUpdate(sql, users) { (id, name) ->
 *         bind(0, name)
 *         bind(1, id)
 *     }
 *     .collect { rowsUpdated ->
 *         println("Rows updated: $rowsUpdated")
 *     }
 * ```
 */
fun <T> Connection.singleUpdate(
    sql: String,
    upstream: Flow<T>,
    concurrency: Int = 1,
    prepare: Statement.(T) -> Unit = {}
): Flow<Long> =
    upstream
        .mapAsync(concurrency) { item ->
            createStatement(sql)
                .also { statement -> prepare(statement, item) }
                .execute()
                .asFlow()
        }
        .flattenFlow()
        .flatMapFlow { it.rowsUpdated.asFlow() }

/**
 * Extension function for Flow<Result> that emits the number of updated rows.
 *
 * @return A Flow<Long> emitting the number of updated rows for each Result.
 */
fun Flow<Result>.rowsUpdated(): Flow<Long> =
    flatMapFlow { it.rowsUpdated.asFlow() }

/**
 * Extension function for Flow<Result> that emits the rows retrieved by a query.
 *
 * @return A Flow<Row> emitting the rows for each Result.
 */
fun Flow<Result>.asRows(): Flow<Row> =
    flatMapFlow { it.map { r, _ -> r }.asFlow() }

/**
 * Extension function for Flow<Result> that maps each row to a custom type using the provided
 * lambda function.
 *
 * @param f A suspend function that takes a Row as input and returns an instance of type T.
 * @return A Flow<T> emitting the transformed rows for each Result.
 *
 * Example usage:
 *
 * ```
 * val connection: Connection = // Obtain a connection from your R2DBC connection factory
 * val sql = "SELECT * FROM users"
 *
 * connection
 *     .query(sql)
 *     .asRows()
 *     .mapRow { row -> User(id = row["id"], name = row["name"], age = row["age"]) }
 *     .collect { user -> println("User: ${user.name}, Age: ${user.age}") }
 * ```
 */
fun <T> Flow<Result>.mapRow(f: suspend (Row) -> T): Flow<T> =
    flatMapFlow { it.map { r, _ -> r }.asFlow() }
        .map { f(it) }

/**
 * Executes a batch update with the specified SQL statement and values from a flow of items.
 * This function chunks the items in the flow and executes the chunks concurrently.
 * Optionally, it can return generated values (e.g. auto-generated keys) from the inserted records.
 *
 * @param sql The SQL statement to execute
 * @param upstream The flow of items to insert/update
 * @param returning The type of generated values to return (if any)
 * @param concurrency The number of concurrent database connections to use
 * @param groupStrategy The chunking strategy to use when batching updates
 * @param prepare A function to prepare the SQL statement before executing it for a given item from the upstream flow
 *
 * @return a [Flow] of [Result]s
 *
 * Example usage:
 *
 * ```
 * val connection: Connection = // Obtain a connection from your R2DBC connection factory
 *
 * val sql = "INSERT INTO users (id, name) VALUES ($1, $2);"
 * val users = flowOf(User(1, "John"), User(2, "Jane"), User(3, "Bob"))
 *
 * val results = connection.batchUpdate(sql, users) { user ->
 *     bind("$1", user.id)
 *     bind("$2", user.name)
 * }
 *
 * results.collect { result ->
 *     val count = result.updateCount
 *     val generatedKeys = result.generatedKeys
 *
 *     // Handle results as needed
 * }
 * ```
 */
fun <T> Connection.batchUpdate(
    sql: String,
    upstream: Flow<T>,
    returning: Returning = Returning.Default,
    concurrency: Int = 1,
    groupStrategy: GroupStrategy = GroupStrategy.TimeWindow(100, 250.milliseconds),
    prepare: Statement.(T) -> Unit = {}
): Flow<Result> =
    upstream
        .chunked(groupStrategy)
        .mapAsync(concurrency) { items ->
            createStatement(sql)
                .let {
                    when (returning) {
                        is Returning.GeneratedValues -> it.returnGeneratedValues(*returning.columns.toTypedArray())
                        Returning.Default -> it
                    }
                }
                .also { statement ->
                    items.forEachIndexed { index, item ->
                        with(statement) {
                            prepare(item)

                            if ((index + 1) < items.size) {
                                add()
                            }
                        }
                    }
                }
                .returnGeneratedValues()
                .execute()
                .asFlow()
        }
        .flattenFlow()

/**
 * Executes a batch update for a specified [upstream] flow of items using a given [groupStrategy] and [concurrency].
 * The [query] function transforms each item in the [upstream] flow into a respective SQL query string.
 * Returns a flow of long values representing the number of rows updated in the database for each chunk.
 *
 * @param upstream The flow of items to insert/update
 * @param concurrency The number of concurrent database connections to use
 * @param groupStrategy The chunking strategy to use when batching updates
 * @param query the function that transforms each item into a SQL query
 *
 * @return a [Flow] of [Result]s
 *
 * Example usage:
 *
 * ```
 *  val connection: Connection = // Obtain a connection from your R2DBC connection factory
 *
 *  val queries = listOf(
 *      "INSERT INTO users (name, age) VALUES ('Alice', 25)",
 *      "INSERT INTO users (name, age) VALUES ('Bob', 30)"
 *  )
 *
 *  val flow = queries.asFlow()
 *
 *  val result = connection.batchUpdate(flow) { it }
 *
 *  result.collect { println("Updated $it rows") } // prints "Updated 1 rows" for each item in the upstream flow
 * ```
 */
fun <T> Connection.batchUpdate(
    upstream: Flow<T>,
    concurrency: Int = 1,
    groupStrategy: GroupStrategy = GroupStrategy.TimeWindow(100, 250.milliseconds),
    query: (T) -> String
): Flow<Result> =
    upstream
        .chunked(groupStrategy)
        .mapAsync(concurrency) { items ->
            createBatch()
                .also { batch -> items.forEach { batch.add(query(it)) } }
                .execute()
                .asFlow()
        }
        .flattenFlow()
