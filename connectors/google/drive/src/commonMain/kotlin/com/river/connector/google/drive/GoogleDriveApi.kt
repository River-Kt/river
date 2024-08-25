package com.river.connector.google.drive

import com.river.connector.google.internal.CachedToken
import com.river.core.ExperimentalRiverApi
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

@ExperimentalRiverApi
class GoogleDriveApi(
    serviceAccount: JsonObject,
    internal val httpClient: HttpClient = defaultHttpClient,
    internal val baseUrl: String = defaultBaseUrl,
) {
    companion object {
        internal const val defaultBaseUrl =
            "https://www.googleapis.com/drive"

        internal val defaultHttpClient =
            HttpClient {
                install(ContentNegotiation) {
                    json(Json {
                        ignoreUnknownKeys = true
                    })
                }
            }

        operator fun CoroutineScope.invoke(
            serviceAccount: JsonObject,
            httpClient: HttpClient = defaultHttpClient,
            baseUrl: String = defaultBaseUrl,
        ) = GoogleDriveApi(serviceAccount, httpClient, baseUrl)
    }

    private val bearerTokenActor = CachedToken(
        serviceAccount = serviceAccount,
        scope = "https://www.googleapis.com/auth/drive"
    )

    internal suspend fun currentToken() =
        bearerTokenActor.currentToken()
}
