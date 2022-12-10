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

fun <T> Flow<T>.toJson(
    objectMapper: ObjectMapper = defaultObjectMapper
): Flow<JsonNode> = map { objectMapper.valueToTree(it) }

fun <T> Flow<T>.toJsonString(
    pretty: Boolean = false,
    objectMapper: ObjectMapper = defaultObjectMapper
): Flow<String> = toJson(objectMapper).map { if (pretty) it.toPrettyString() else it.toString() }

inline fun <reified T> Flow<String>.fromJsonString(
    objectMapper: ObjectMapper = defaultObjectMapper
): Flow<T> = map { objectMapper.readValue(it, T::class.java) }

inline fun <reified T> Flow<JsonNode>.fromJson(
    objectMapper: ObjectMapper = defaultObjectMapper
): Flow<T> = map { objectMapper.treeToValue(it, T::class.java) }

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
