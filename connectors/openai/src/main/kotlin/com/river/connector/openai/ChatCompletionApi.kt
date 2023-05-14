package com.river.connector.openai

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.module.kotlin.jsonMapper
import com.river.connector.format.json.asParsedJson
import com.river.core.asByteArray
import com.river.core.asString
import com.river.core.flatten
import com.river.connector.http.ofFlow
import com.river.connector.http.post
import com.river.connector.http.coSend
import kotlinx.coroutines.flow.*
import java.net.http.HttpClient

class ChatCompletionApi(
    private val apiKey: String,
    private val baseUrl: String = ChatCompletionApi.baseUrl,
    private val client: HttpClient = ChatCompletionApi.httpClient,
    private val objectMapper: ObjectMapper = ChatCompletionApi.objectMapper
) {
    companion object {
        private val baseUrl = "https://api.openai.com/v1"

        private val httpClient = HttpClient.newHttpClient()

        private val objectMapper: ObjectMapper = jsonMapper {
            propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
            serializationInclusion(Include.NON_NULL)
            findAndAddModules()
        }
    }

    suspend fun chatCompletions(request: ChatCompletion): Flow<TextCompletionResponse> {
        val url = "$baseUrl/chat/completions"

        val httpRequest = post(url) {
            contentType("application/json")
            header("Authorization", "Bearer $apiKey")

            stringBody(objectMapper.writeValueAsString(request))
        }

        return httpRequest
            .coSend(ofFlow, client)
            .body()
            .asByteArray()
            .asString()
            .map { it.split("data:") }
            .flatten()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .takeWhile { it != "[DONE]" }
            .asParsedJson<TextCompletionResponse>(objectMapper)
    }
}
