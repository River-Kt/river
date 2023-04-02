package io.river.connector.format.json

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.core.JsonToken.*
import com.fasterxml.jackson.core.async.ByteArrayFeeder
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.river.core.flatten
import kotlinx.coroutines.flow.*

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
 *  peopleFlow.toJson().collect { jsonNode -> println(jsonNode) }
 * ```
 */
fun <T> Flow<T>.toJson(
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
 *      .toJsonString(pretty = true)
 *      .collect { jsonString -> println(jsonString) }
 * ```
 */
fun <T> Flow<T>.toJsonString(
    pretty: Boolean = false,
    objectMapper: ObjectMapper = defaultObjectMapper
): Flow<String> = toJson(objectMapper).map { if (pretty) it.toPrettyString() else it.toString() }

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
 *  jsonStringFlow.fromJsonString<Person>().collect { person -> println(person) }
 * ```
 */
inline fun <reified T> Flow<String>.fromJsonString(
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
 *  jsonNodeFlow.fromJson<Person>().collect { person -> println(person) }
 * ```
 */
inline fun <reified T> Flow<JsonNode>.fromJson(
    objectMapper: ObjectMapper = defaultObjectMapper
): Flow<T> = map { objectMapper.treeToValue(it, T::class.java) }

/**
 * Creates a flow of JsonNode objects by parsing the input byte array flow as JSON.
 *
 * This function can parse JSON data in a streaming fashion, as it can handle each element as small chunks of bytes,
 * waiting for the whole JSON root node to be complete before emitting this node, unlike the `fromJson` function that requires
 * each element to be a whole valid JSON string.
 *
 * The ability to parse streamed JSON data is highly beneficial when working with non-standardized JSON data.
 * Regardless of indentation, whether it is a JSON array or multiple JSON objects separated by line breaks,
 * this function can effectively emit each root JSON node.
 *
 * @param objectMapper The ObjectMapper instance to use for processing JSON data. Defaults to defaultObjectMapper.
 *
 * @return A flow of root-level JsonNode objects.
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
fun Flow<ByteArray>.rootJsonNodes(
    objectMapper: ObjectMapper = defaultObjectMapper
): Flow<JsonNode> = flow {
    var first: JsonToken? = null

    val parser: JsonParser = JsonFactory().createNonBlockingByteArrayParser()
    val feeder = parser.nonBlockingInputFeeder as ByteArrayFeeder

    var buffer: String = ""

    var depth = 0

    onCompletion { parser.close() }
        .onEach { feeder.feedInput(it, 0, it.size) }
        .onEach { buffer += String(it) }
        .map {
            buildList {
                var token = parser.nextToken()
                while (token != NOT_AVAILABLE) {
                    add(token)
                    token = parser.nextToken()
                }
            }
        }
        .flatten()
        .collect { token ->
            if (first == null) first = token

            suspend fun emitNodeBufferedNode() {
                buffer = buffer.trimStart().trimEnd()

                if (buffer.first() in listOf(',', ']', '['))
                    buffer = buffer.drop(1).trimStart().trimEnd()

                if (buffer.lastOrNull() == ',' || (buffer.first() != '[' && buffer.lastOrNull() == ']')) {
                    buffer = buffer.dropLast(1)
                }

                emit(objectMapper.readTree(buffer))
                buffer = ""
            }

            when (token) {
                START_OBJECT,
                START_ARRAY -> {
                    depth += 1
                }

                END_OBJECT,
                END_ARRAY -> {
                    depth -= 1
                    if (depth == 1) emitNodeBufferedNode()
                }

                VALUE_STRING,
                VALUE_NUMBER_INT,
                VALUE_NUMBER_FLOAT,
                VALUE_TRUE,
                VALUE_FALSE,
                VALUE_NULL -> {
                    if (depth == 1) emitNodeBufferedNode()
                }

                null,
                FIELD_NAME,
                VALUE_EMBEDDED_OBJECT,
                NOT_AVAILABLE -> {
                }
            }
        }
}
