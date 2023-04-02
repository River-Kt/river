package com.river.core

import kotlinx.coroutines.flow.*

/**
 * Intersperses the elements of the [Flow] with the specified [between] element.
 *
 * @param between An element of type [T] to insert between the flow elements.
 *
 * @return A new [Flow] with the [between] element inserted between the original flow elements.
 *
 * Example usage:
 *
 * ```
 *  flowOf(1, 2, 3)
 *      .intersperse(between = 0)
 *      .collect(::println) //1, 0, 2, 0, 3
 * ```
 */
fun <T> Flow<T>.intersperse(
    between: T
): Flow<T> = intersperse(start = null, between = between, end = null)

/**
 * Intersperses the elements of the [Flow] with the specified [start], [between], and [end] elements.
 *
 * @param start An optional element of type [T] to insert at the beginning of the flow. Defaults to null.
 * @param between An element of type [T] to insert between the flow elements.
 * @param end An optional element of type [T] to insert at the end of the flow. Defaults to null.
 *
 * @return A new [Flow] with the [start], [between], and [end] elements inserted between and around the original flow elements.
 *
 * Example usage:
 *
 * ```
 *  flowOf(1, 2, 3)
 *      .intersperse(start = 0, between = -1, end = 4)
 *      .collect(::println) //0, 1, -1, 2, -1, 3, 4
 */
fun <T> Flow<T>.intersperse(
    start: T? = null,
    between: T,
    end: T? = null
): Flow<T> =
    flow {
        var first = true
        var last = false

        if (start != null) emit(start)

        onCompletion {
            last = true
            if (end != null) emit(end)
        }.collect {
            if (!first && !last) emit(between)
            emit(it)
            first = false
        }
    }

/**
 * Joins the elements of the [Flow] into a single string using the provided [f] function
 * to transform each element to a string.
 *
 * @param f A suspend function that transforms an element of type [T] to a [String]. Defaults to calling `toString()` on the element.
 *
 * @return A [String] containing the concatenated strings of the flow elements.
 *
 * Example usage:
 *
 * ```
 *  flowOf(1, 2, 3, 6, 5, 4)
 *      .joinToString { "$it" }
 *      .also(::println) //123654
 * ```
 */
suspend fun <T> Flow<T>.joinToString(
    f: suspend (T) -> String = { it.toString() }
): String =
    map(f).fold("") { acc, element -> acc + element }

/**
 * Joins the elements of the [Flow] into a single string, separated by the specified [between] string,
 * using the provided [f] function to transform each element to a string.
 *
 * @param between A [String] used as a separator between the flow elements.
 * @param f A suspend function that transforms an element of type [T] to a [String].
 *
 * @return A [String] containing the concatenated strings of the flow elements, separated by the [between] string.
 *
 * Example usage:
 *
 * ```
 *  flowOf(1, 2, 3, 6, 5, 4)
 *      .joinToString(", ") { "$it" }
 *      .also(::println) //1, 2, 3, 6, 5, 4
 * ```
 */
suspend fun <T> Flow<T>.joinToString(
    between: String,
    f: suspend (T) -> String
): String =
    map(f)
        .intersperse(between)
        .fold("") { acc, element -> acc + element }

/**
 * Joins the elements of the [Flow] into a single string, enclosed by the specified [start] and [end] strings,
 * and separated by the specified [between] string. Uses the provided [f] function to transform each element to a string.
 *
 * @param start A [String] used at the beginning of the resulting string.
 * @param between A [String] used as a separator between the flow elements.
 * @param end A [String] used at the end of the resulting string.
 * @param f A suspend function that transforms an element of type [T] to a [String].
 *
 * @return A [String] containing the concatenated strings of the flow elements, enclosed by [start] and [end], and separated by the [between] string.
 *
 * Example usage:
 *
 * ```
 *  flowOf(1, 2, 3, 6, 5, 4)
 *      .joinToString("[", ", ", "]") { "$it" }
 *      .also(::println) //[1, 2, 3, 6, 5, 4]
 * ```
 */
suspend fun <T> Flow<T>.joinToString(
    start: String,
    between: String,
    end: String,
    f: suspend (T) -> String
): String =
    map(f)
        .intersperse(start, between, end)
        .fold("") { acc, element -> acc + element }
