package com.river.connector.openai

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.module.kotlin.jsonMapper
import java.net.http.HttpClient

class OpenAIApi(
    internal val apiKey: String,
    internal val baseUrl: String = OpenAIApi.baseUrl,
    internal val client: HttpClient = OpenAIApi.httpClient,
    internal val objectMapper: ObjectMapper = OpenAIApi.objectMapper
) {
    companion object {
        private const val baseUrl = "https://api.openai.com/v1"

        private val httpClient = HttpClient.newHttpClient()

        private val objectMapper: ObjectMapper = jsonMapper {
            propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
            serializationInclusion(Include.NON_NULL)
            findAndAddModules()
        }
    }
}
