package com.river.connector.openai.chatgpt

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.module.kotlin.jsonMapper
import com.river.connector.format.json.fromJsonString
import com.river.core.asByteArray
import com.river.core.asString
import com.river.core.flatten
import com.river.util.http.ofFlow
import com.river.util.http.post
import com.river.util.http.send
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import java.net.http.HttpClient

enum class Model(
    val type: String,
    val maxTokens: Int,
) {
    GPT_4("gpt-4", 4096),
    GPT_3_5_TURBO("gpt-3.5-turbo", 2048),
    GPT_3_5_TURBO_0301("gpt-3.5-turbo-0301", 2048);

    @JsonValue
    fun value() = type
}

data class Temperature(@JsonValue val value: Double = 1.0) {
    init {
        require(value <= 2) { "temperature cannot be higher than 2" }
        require(value >= 0) { "temperature cannot be lower than 0" }
    }
}

class ChatGPTApi(
    private val apiKey: String,
    private val baseUrl: String = "https://api.openai.com/v1",
    private val client: HttpClient = HttpClient.newHttpClient(),
    private val objectMapper: ObjectMapper = jsonMapper {
        propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
        disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
        serializationInclusion(Include.NON_NULL)
        findAndAddModules()
    }
) {
    suspend fun chatCompletions(request: ChatCompletion): Flow<TextCompletionResponse> {
        val url = "$baseUrl/chat/completions"

        val httpRequest = post(url) {
            contentType("application/json")
            header("Authorization", "Bearer $apiKey")

            stringBody(objectMapper.writeValueAsString(request))
        }

        return httpRequest
            .send(ofFlow, client)
            .body()
            .asByteArray()
            .asString()
            .map { it.split("data:") }
            .flatten()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .takeWhile { it != "[DONE]" }
            .fromJsonString<TextCompletionResponse>(objectMapper)
    }
}
