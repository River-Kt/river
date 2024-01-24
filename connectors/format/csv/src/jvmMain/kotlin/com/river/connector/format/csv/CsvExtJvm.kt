package com.river.connector.format.csv

import kotlinx.coroutines.flow.Flow

/**
 * Converts a flow of objects into a flow of CSV strings, using reflection to automatically
 * map object fields to CSV columns.
 *
 * @param appendHeader Whether to append the header row to the CSV output. Defaults to true.
 * @param delimiter The delimiter to use for separating values in the CSV. Defaults to ";".
 *
 * @return A flow of CSV strings.
 *
 * Example usage:
 *
 * ```
 *  data class Person(val name: String, val age: Int)
 *  val peopleFlow = flowOf(Person("Alice", 30), Person("Bob", 25))
 *
 *  peopleFlow
 *      .csv(appendHeader = true, delimiter = ";")
 *      .collect { line -> println(line) }
 * ```
 */
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
