package com.river.connector.github.internal

import com.river.connector.github.model.query.PageableQuery
import com.river.core.*
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.withIndex

internal val regex = "([a-z])([A-Z]+)".toRegex()
internal const val replacement = "$1_$2"

internal fun String.snakeCase(): String =
    this.replace(regex, replacement).lowercase()

internal fun <T, Q : PageableQuery> paginatedFlowApi(
    filter: Q.() -> Unit,
    concurrency: Int,
    f: suspend (Q.() -> Unit) -> List<T>
) = indefinitelyRepeat(filter)
    .withIndex()
    .map { (index, filter) -> (index + 1) to filter  }
    .mapAsync(concurrency) { (page, filter) ->
        f {
            filter()
            this.page = page
        }
    }
    .takeWhile { it.isNotEmpty() }
    .flatten()
