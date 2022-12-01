@file:OptIn(FlowPreview::class, FlowPreview::class)

package io.github.gabfssilva.river.r2dbc

import io.github.gabfssilva.river.core.ChunkStrategy
import io.github.gabfssilva.river.core.chunked
import io.github.gabfssilva.river.core.mapParallel
import io.r2dbc.spi.Batch
import io.r2dbc.spi.Connection
import io.r2dbc.spi.Statement
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flattenConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import kotlin.time.Duration.Companion.milliseconds


fun Connection.executeStatementFlow(
    upstream: Flow<Statement>,
    parallelism: Int = 1
) = upstream
        .mapParallel(parallelism) { it.execute().asFlow()}
        .flattenConcat()

fun Connection.executeBatchedStatementFlow(
    upstream: Flow<String>,
    batch: Batch,
    parallelism: Int = 1,
    chunkStrategy: ChunkStrategy = ChunkStrategy.TimeWindow(10, 250.milliseconds)
)=
    upstream
        .chunked(chunkStrategy)
        .mapParallel(parallelism) {statement ->
            batch.apply {
                statement.map { add(it) }
            }.execute().asFlow()
        }.flattenConcat()
