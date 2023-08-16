package com.river.connector.openai

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.module.kotlin.jsonMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.river.connector.format.json.asParsedJson
import com.river.connector.http.*
import com.river.core.ExperimentalRiverApi
import com.river.core.flatMapIterable
import com.river.core.joinToString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.takeWhile
import java.net.http.HttpClient
import java.net.http.HttpRequest

@ExperimentalRiverApi
class OpenAIClient(
    internal val apiKey: String,
    internal val baseUrl: String = OpenAIClient.baseUrl,
    internal val client: HttpClient = httpClient,
    internal val objectMapper: ObjectMapper = OpenAIClient.objectMapper
) {
    companion object {
        private const val baseUrl = "https://api.openai.com/v1"

        private val httpClient =
            HttpClient
                .newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build()

        private val objectMapper: ObjectMapper = jsonMapper {
            propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
            serializationInclusion(Include.NON_NULL)
            findAndAddModules()
        }
    }

    internal fun CustomHttpRequestBuilder.jsonBody(request: Any) {
        contentType("application/json")
        stringBody(objectMapper.writeValueAsString(request))
    }

    internal inline fun <reified T> ofJson() =
        ofByteArray.map { lazy { objectMapper.readValue<T>(it) } }

    internal inline fun <reified T> completionStream(
        noinline httpRequest: suspend () -> HttpRequest
    ) = flow {
        val response =
            client.coSend(
                bodyHandler = ofStringFlow,
                request = httpRequest
            )

        require(response.statusCode() != 401) {
            "You're not authorized to use this API: ${response.statusCode()}: ${response.body().joinToString()}"
        }

        require(response.statusCode() == 200) {
            "The API responded an invalid http status: ${response.statusCode()}: ${response.body().joinToString()}"
        }

        val serverSentEvents: Flow<ServerSentEvent> = response.body().parseAsServerSentEvents()

        serverSentEvents
            .flatMapIterable { it.data }
            .takeWhile { it != "[DONE]" }
            .asParsedJson<T>(objectMapper)
            .also { emitAll(it) }
    }
}
