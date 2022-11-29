@file:OptIn(FlowPreview::class, FlowPreview::class)

package io.github.gabfssilva.river.r2dbc

import io.github.gabfssilva.river.core.mapParallel
import io.r2dbc.spi.Connection
import io.r2dbc.spi.Statement
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flattenConcat
import kotlinx.coroutines.reactive.asFlow


fun Connection.executeStatementFlow(
    upstream: Flow<Statement>,
    parallelism: Int = 1
) = upstream
        .mapParallel(parallelism) { it.execute().asFlow()}
        .flattenConcat()
