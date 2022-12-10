@file:OptIn(FlowPreview::class)

package io.river.connector.rdbms.r2dbc

import io.river.core.mapParallel
import io.r2dbc.spi.Connection
import io.r2dbc.spi.Statement
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flattenConcat
import kotlinx.coroutines.reactive.asFlow


typealias ResultRow = Map<String, Any?>

fun Connection.query(
    sql: String
): Flow<ResultRow> =
    createStatement(sql)
        .execute()
        .asFlow()
        .flatMapConcat {
            it.map { row, rowMetadata ->
                rowMetadata.columnMetadatas.associate { metadata ->
                    val columnName = metadata.name
                    columnName to row.get(columnName)
                }
            }.asFlow()
        }

fun Connection.singleUpdate(
    sql: String
): Flow<Long> =
    createStatement(sql)
        .execute()
        .asFlow()
        .flatMapConcat { it.rowsUpdated.asFlow() }


fun <T> Connection.singleUpdate(
    sql: String,
    upstream: Flow<T>,
    parallelism: Int = 1,
    prepare: Statement.(T) -> Unit = {}
): Flow<Long> = upstream
    .mapParallel(parallelism) { item ->
        createStatement(sql).also { statement ->
            prepare(statement, item)
        }.execute().asFlow()
    }
    .flattenConcat()
    .flatMapConcat { it.rowsUpdated.asFlow() }
