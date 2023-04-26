package com.river.connector.format.csv

import kotlinx.coroutines.flow.*

/**
 * Converts a flow of objects into a flow of raw CSV strings, using a provided function to
 * convert each object into a list of strings.
 *
 * @param headers The list of header names for the CSV.
 * @param delimiter The delimiter to use for separating values in the CSV. Defaults to ";".
 * @param f The function to convert each object into a list of strings.
 *
 * @return A flow of raw CSV strings.
 *
 * Example usage:
 *
 * ```
 *  data class Person(val name: String, val age: Int)
 *  val peopleFlow = flowOf(Person("Alice", 30), Person("Bob", 25))
 *
 *  peopleFlow
 *      .rawCsv(listOf("Name", "Age"), delimiter = ";") { person ->
 *          listOf(person.name, person.age.toString())
 *      }
 *      .collect { line -> println(line) }
 * ```
 */
fun <T> Flow<T>.rawCsv(
    headers: List<String>,
    delimiter: String = ";",
    f: (T) -> List<String>
): Flow<String> =
    merge(
        if (headers.isEmpty()) emptyFlow() else flowOf(headers.joinToString(delimiter)),
        map { f(it).joinToString(delimiter) }
    )

/**
 * Converts a flow of objects into a flow of raw CSV strings, using a provided function to
 * convert each object into a list of strings.
 *
 * @param headers The vararg of header names for the CSV.
 * @param delimiter The delimiter to use for separating values in the CSV. Defaults to ";".
 * @param f The function to convert each object into a list of strings.
 *
 * @return A flow of raw CSV strings.
 *
 * Example usage:
 *
 * ```
 *  data class Person(val name: String, val age: Int)
 *  val peopleFlow = flowOf(Person("Alice", 30), Person("Bob", 25))
 *
 *  peopleFlow
 *      .rawCsv("Name", "Age", delimiter = ";") { person ->
 *          listOf(person.name, person.age.toString())
 *      }
 *      .collect { line -> println(line) }
 * ```
 */
fun <T> Flow<T>.rawCsv(
    vararg headers: String,
    delimiter: String = ";",
    f: (T) -> List<String>
): Flow<String> = rawCsv(headers.toList(), delimiter, f)

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

/**
 * Parses the CSV data from a flow of strings and emits each row as a list of strings.
 *
 * @param delimiter The delimiter to use for separating values in the CSV. Defaults to ";".
 *
 * @return A flow of lists of strings, each representing a row of parsed CSV values.
 *
 * Example usage:
 *
 * ```
 *  val csvFlow = flowOf("Name;Age", "Alice;30", "Bob;25")
 *  csvFlow.parseCsv().collect { row -> println(row) }
 * ```
 */
fun Flow<String>.parseCsv(
    delimiter: String = ";"
): Flow<List<String>> =
    map { it.split(delimiter) }

/**
 * Parses the CSV data from a flow of strings and emits each row as a custom object,
 * converted using the provided function.
 *
 * @param delimiter The delimiter to use for separating values in the CSV. Defaults to ";".
 * @param f The function to convert the parsed row (list of strings) into a custom object.
 *
 * @return A flow of custom objects, each representing a row of parsed CSV values.
 *
 * Example usage:
 *
 * ```
 *  data class Person(val name: String, val age: Int)
 *
 *  val csvFlow = flowOf("Name;Age", "Alice;30", "Bob;25")
 *
 *  csvFlow
 *      .parseCsv { (name, age) -> Person(name, age.toInt()) }
 *      .collect { person -> println(person) }
 * ```
 */
fun <T> Flow<String>.parseCsv(
    delimiter: String = ";",
    f: (List<String>) -> T
): Flow<T> = parseCsv(delimiter).map { f(it) }

/**
 * Parses the CSV data with headers from a flow of strings and emits each row as a map
 * where the keys are the headers and the values are the corresponding row values.
 *
 * @param delimiter The delimiter to use for separating values in the CSV. Defaults to ";".
 *
 * @return A flow of maps, each representing a row of parsed CSV values with headers as keys.
 *
 * Example usage:
 *
 * ```
 *  val csvFlow = flowOf("Name;Age", "Alice;30", "Bob;25")
 *
 *  csvFlow
 *      .parseCsvWithHeaders()
 *      .collect { row -> println(row) }
 * ```
 */
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

/**
 * Parses the CSV data with headers from a flow of strings and emits each row as a custom object,
 * converted using the provided function.
 *
 * @param delimiter The delimiter to use for separating values in the CSV. Defaults to ";".
 * @param f The function to convert the parsed row (map of header-value pairs) into a custom object.
 *
 * @return A flow of custom objects, each representing a row of parsed CSV values with headers as keys.
 *
 * Example usage:
 *
 * ```
 *  data class Person(val name: String, val age: Int)
 *
 *  val csvFlow = flowOf("Name;Age", "Alice;30", "Bob;25")
 *
 *  csvFlow
 *      .parseCsvWithHeaders { row -> Person(checkNotNull(row["Name"]), checkNotNull(row["Age"]?.toInt())) }
 *      .collect { person -> println(person) }
 * ```
 */
fun <T> Flow<String>.parseCsvWithHeaders(
    delimiter: String = ";",
    f: (Map<String, String>) -> T
): Flow<T> =
    parseCsvWithHeaders(delimiter)
        .map { f(it) }
