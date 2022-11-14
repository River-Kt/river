package io.github.gabfssilva.river.format.csv

import kotlinx.coroutines.flow.*

fun <T> Flow<T>.rawCsv(
    headers: List<String>,
    separator: String = ";",
    f: (T) -> List<String>
): Flow<String> =
    merge(
        if (headers.isEmpty()) emptyFlow() else flowOf(headers.joinToString(separator)),
        map { f(it).joinToString(separator) }
    )

fun <T> Flow<T>.rawCsv(
    vararg headers: String,
    separator: String = ";",
    f: (T) -> List<String>
): Flow<String> = rawCsv(headers.toList(), separator, f)

inline fun <reified T> Flow<T>.csv(
    appendHeader: Boolean = true,
    separator: String = ";"
): Flow<String> =
    T::class.java
        .declaredFields
        .onEach { it.trySetAccessible() }
        .let { fields ->
            val headers =
                if (appendHeader) fields.map { it.name }
                else emptyList()

            rawCsv(headers, separator) { i -> fields.map { it.get(i).toString() } }
        }
