package io.river.connector.format.csv

import io.river.core.lines
import kotlinx.coroutines.flow.*

fun <T> Flow<T>.rawCsv(
    headers: List<String>,
    delimiter: String = ";",
    f: (T) -> List<String>
): Flow<String> =
    merge(
        if (headers.isEmpty()) emptyFlow() else flowOf(headers.joinToString(delimiter)),
        map { f(it).joinToString(delimiter) }
    )

fun <T> Flow<T>.rawCsv(
    vararg headers: String,
    delimiter: String = ";",
    f: (T) -> List<String>
): Flow<String> = rawCsv(headers.toList(), delimiter, f)

inline fun <reified T> Flow<T>.csv(
    appendHeader: Boolean = true,
    delimiter: String = ";"
): Flow<String> =
    T::class.java
        .declaredFields
        .onEach { it.trySetAccessible() }
        .let { fields ->
            val headers =
                if (appendHeader) fields.map { it.name }
                else emptyList()

            rawCsv(headers, delimiter) { i -> fields.map { it.get(i).toString() } }
        }

fun Flow<String>.parseCsv(
    delimiter: String = ";"
): Flow<List<String>> =
    lines().map { it.split(delimiter) }

fun <T> Flow<String>.parseCsv(
    delimiter: String = ";",
    f: (List<String>) -> T
): Flow<T> = parseCsv(delimiter).map { f(it) }

fun Flow<String>.parseCsvWithHeaders(
    delimiter: String = ";"
): Flow<Map<String, String>> =
    flow {
        var headers = emptyList<String>()

        parseCsv(delimiter)
            .collect {
                if (headers.isEmpty()) {
                    headers = it
                } else {
                    emit((headers zip it).toMap())
                }
            }
    }

fun <T> Flow<String>.parseCsvWithHeaders(
    delimiter: String = ";",
    f: (Map<String, String>) -> T
): Flow<T> =
    parseCsvWithHeaders(delimiter)
        .map { f(it) }
