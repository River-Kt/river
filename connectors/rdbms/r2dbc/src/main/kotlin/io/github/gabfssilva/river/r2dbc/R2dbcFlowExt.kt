@file:OptIn(FlowPreview::class)

package io.github.gabfssilva.river.r2dbc

import io.github.gabfssilva.river.core.mapParallel
import io.r2dbc.spi.Connection
import io.r2dbc.spi.Statement
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flattenConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow


typealias ResultRow = Map<String, Any?>

fun Connection.query(
    sql: String
): Flow<ResultRow> =
    createStatement(sql)
        .execute()
        .asFlow()
        .map {
            it.map { row, rowMetadata ->
                rowMetadata.columnMetadatas.associate { metadata ->
                    val columnName = metadata.name
                    columnName to row.get(columnName)
                }
            }.asFlow()
        }.flattenConcat()

fun Connection.singleUpdate(
    sql: String
): Flow<Long> =
    createStatement(sql)
        .execute()
        .asFlow()
        .map { it.rowsUpdated.asFlow() }
        .flattenConcat()


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
    .map { it.rowsUpdated.asFlow() }
    .flattenConcat()
