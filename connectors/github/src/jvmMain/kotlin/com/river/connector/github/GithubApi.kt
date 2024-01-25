package com.river.connector.github

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.module.kotlin.jsonMapper
import com.river.connector.http.CustomHttpRequestBuilder
import java.net.http.HttpClient

class GithubApi(
    internal val apiKey: String,
    internal val baseUrl: String = GithubApi.baseUrl,
    internal val client: HttpClient = httpClient,
    internal val objectMapper: ObjectMapper = GithubApi.objectMapper
) {
    companion object {
        internal const val baseUrl = "https://api.github.com"
        internal val httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()
        internal val objectMapper = jsonMapper {
            propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
            findAndAddModules()
        }
    }

    internal fun CustomHttpRequestBuilder.defaultHeaders() {
        accept("application/vnd.github+json")
        authorization { bearer(apiKey) }
    }
}
