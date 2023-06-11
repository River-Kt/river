package com.river.connector.format.json

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonToken.*
import com.fasterxml.jackson.core.async.ByteArrayFeeder
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import com.river.core.asString
import com.river.core.flattenIterable
import com.river.core.lines
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

val defaultObjectMapper by lazy { jacksonObjectMapper() }

/**
 * Converts a flow of objects into a flow of JSON nodes, using the provided ObjectMapper.
 *
 * @param objectMapper The ObjectMapper instance to use for converting objects to JSON nodes.
 *                     Defaults to defaultObjectMapper.
 *
 * @return A flow of JsonNode objects.
 *
 * Example usage:
 *
 * ```
 *  data class Person(val name: String, val age: Int)
 *  val peopleFlow = flowOf(Person("Alice", 30), Person("Bob", 25))
 *
 *  peopleFlow.asJsonNode().collect { jsonNode -> println(jsonNode) }
 * ```
 */
fun <T> Flow<T>.asJsonNode(
    objectMapper: ObjectMapper = defaultObjectMapper
): Flow<JsonNode> = map { objectMapper.valueToTree(it) }

/**
 * Converts a flow of objects into a flow of JSON strings, using the provided ObjectMapper.
 *
 * @param pretty Whether to output the JSON strings in a human-readable format. Defaults to false.
 * @param objectMapper The ObjectMapper instance to use for converting objects to JSON strings.
 *                     Defaults to defaultObjectMapper.
 *
 * @return A flow of JSON strings.
 *
 * Example usage:
 *
 * ```
 *  data class Person(val name: String, val age: Int)
 *  val peopleFlow = flowOf(Person("Alice", 30), Person("Bob", 25))
 *
 *  peopleFlow
 *      .asJsonString(pretty = true)
 *      .collect { jsonString -> println(jsonString) }
 * ```
 */
fun <T> Flow<T>.asJsonString(
    pretty: Boolean = false,
    objectMapper: ObjectMapper = defaultObjectMapper
): Flow<String> = asJsonNode(objectMapper).map { if (pretty) it.toPrettyString() else it.toString() }

/**
 * Converts a flow of JSON strings into a flow of objects of the specified type, using the provided ObjectMapper.
 *
 * @param objectMapper The ObjectMapper instance to use for converting JSON strings to objects.
 *                     Defaults to defaultObjectMapper.
 *
 * @return A flow of objects of the specified type.
 *
 * Example usage:
 *
 * ```
 *  data class Person(val name: String, val age: Int)
 *  val jsonStringFlow = flowOf("""{"name":"Alice","age":30}""", """{"name":"Bob","age":25}""")
 *
 *  jsonStringFlow.asParsedJson<Person>().collect { person -> println(person) }
 * ```
 */
inline fun <reified T> Flow<String>.asParsedJson(
    objectMapper: ObjectMapper = defaultObjectMapper
): Flow<T> = map { objectMapper.readValue(it, T::class.java) }

/**
 * Converts a flow of JsonNode objects into a flow of objects of the specified type, using the provided ObjectMapper.
 *
 * @param objectMapper The ObjectMapper instance to use for converting JsonNode objects to objects.
 *                     Defaults to defaultObjectMapper.
 *
 * @return A flow of objects of the specified type.
 *
 * Example usage:
 *
 * ```
 *  data class Person(val name: String, val age: Int)
 *  val objectMapper = ObjectMapper()
 *
 *  val jsonNodeFlow = flowOf(
 *      objectMapper.readTree("""{"name":"Alice","age":30}"""),
 *      objectMapper.readTree("""{"name":"Bob","age":25}""")
 *  )
 *
 *  jsonNodeFlow.asValue<Person>().collect { person -> println(person) }
 * ```
 */
inline fun <reified T> Flow<JsonNode>.asValue(
    objectMapper: ObjectMapper = defaultObjectMapper
): Flow<T> = map { objectMapper.treeToValue(it, T::class.java) }

/**
 * Converts a [Flow] of byte arrays into a [Flow] of parsed JSON objects of a specified type.
 *
 * The function buffers incoming byte arrays, treating them as parts of a JSON Lines formatted string,
 * and processes each JSON object line as soon as it's fully received.
 *
 * This approach is especially useful when consuming data from a slow upstream, such as a network call,
 * as it allows data processing to begin as soon as enough has been received rather than waiting for the entire data set.
 *
 * By not loading all the data into memory at once, it works in a memory-efficient manner.
 *
 * Additionally, it leverages the built-in backpressure handling of Kotlin's [Flow] API to prevent the producer from
 * overwhelming the consumer.
 *
 * @param T The type into which JSON objects should be parsed. This is identified at runtime using reified type parameters.
 * @param objectMapper The `ObjectMapper` to use for JSON parsing. If not specified, a default `ObjectMapper` is used.
 *
 * @return A [Flow] of parsed objects of type `T`.
 *
 * Example usage:
 * ```
 * val flow: Flow<ByteArray> = flowOf(
 *    "{ \"name\": \"John",
 *    " Doe\" }\n",
 *    "{ \"name\": \"Jane Doe\" }\n"
 * ).asByteArray()
 *
 * val parsedFlow: Flow<Map<String, String>> = flow.parseJsonLines()
 *
 * parsedFlow.collect { println(it) }
 * ```
 */
inline fun <reified T> Flow<ByteArray>.parseJsonLines(
    objectMapper: ObjectMapper = defaultObjectMapper
): Flow<T> =
    asString()
        .lines()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .asParsedJson(objectMapper)

/**
 * Converts a [Flow] of byte arrays, representing a JSON array, into a [Flow] of parsed JSON objects of a specified type.
 *
 * This function reads from a [Flow] of byte arrays, treating the incoming data as a JSON array. As the array is streamed in,
 * each JSON object it contains is parsed into an instance of type `T` and emitted as soon as it's fully received.
 *
 * This approach allows for memory-efficient and backpressure-aware processing of large JSON arrays, even when the
 * data source is slow (e.g., a network call).
 *
 * @param T The type into which JSON objects should be parsed. This is identified at runtime using reified type parameters.
 * @param objectMapper The `ObjectMapper` to use for JSON parsing. If not specified, a default `ObjectMapper` is used.
 *
 * @return A [Flow] of parsed objects of type `T`.
 *
 * Example usage:
 *
 * ```
 *  val jsonBytesFlow = flowOf(
 *      "[",
 *      """{"name":"Alice"""",
 *      ""","age":30},""",
 *      """{"name":"Bob"""",
 *      ""","age":25}""",
 *      "]"
 *  ).map { it.toByteArray() }
 *
 *  jsonBytesFlow
 *      .rootJsonNodes()
 *      .collect { jsonNode -> println(jsonNode) }
 * ```
 */
inline fun <reified T : Any> Flow<ByteArray>.parseJsonArray(
    objectMapper: ObjectMapper = defaultObjectMapper
): Flow<T> = flow {
    val typeRef = jacksonTypeRef<T>()

    JsonFactory()
        .createNonBlockingByteArrayParser()
        .use { parser ->
            val feeder = parser.nonBlockingInputFeeder as ByteArrayFeeder
            var buffer = ""

            var depth = 0

            map { it.toList() }
                .flattenIterable()
                .map { arrayOf(it).toByteArray() }
                .collect { bytes ->
                    feeder.feedInput(bytes, 0, bytes.size)
                    buffer += String(bytes)

                    if ((depth == 0 && buffer in listOf("[", "]")) || buffer == ",") {
                        buffer = buffer.drop(1)
                    }

                    var token = parser.nextToken()

                    while (token != NOT_AVAILABLE) {
                        when (token) {
                            START_OBJECT, START_ARRAY -> depth += 1
                            END_OBJECT, END_ARRAY -> depth += -1
                            else -> {}
                        }

                        if (depth == 1 && buffer.isNotBlank()) {
                            if (buffer.trimEnd().last() in listOf(',', ']')) {
                                buffer = buffer.dropLast(1)
                            }

                            runCatching { objectMapper.readValue(buffer.trim(), typeRef) }
                                .onSuccess {
                                    emit(it)
                                    buffer = ""
                                }
                        }

                        token = parser.nextToken()
                    }
                }
        }
}
