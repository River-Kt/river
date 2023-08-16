package com.river.connector.google.drive

import com.fasterxml.jackson.databind.JsonNode
import com.river.connector.google.drive.internal.BearerTokenActor
import com.river.core.ExperimentalRiverApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.net.http.HttpClient

@ExperimentalRiverApi
class GoogleDriveApi(
    private val serviceAccount: JsonNode,
    private val scope: CoroutineScope = defaultCoroutineScope,
    internal val httpClient: HttpClient = defaultHttpClient,
    internal val baseUrl: String = defaultBaseUrl,
) {
    companion object {
        internal const val defaultBaseUrl =
            "https://www.googleapis.com/drive"

        internal val defaultCoroutineScope by lazy {
            CoroutineScope(Dispatchers.Default)
        }

        internal val defaultHttpClient =
            HttpClient
                .newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build()

        operator fun CoroutineScope.invoke(
            serviceAccount: JsonNode,
            httpClient: HttpClient = defaultHttpClient,
            baseUrl: String = defaultBaseUrl,
        ) = GoogleDriveApi(serviceAccount, this, httpClient, baseUrl)
    }

    private val bearerTokenActor =
        BearerTokenActor(serviceAccount, httpClient, scope)

    internal suspend fun currentToken() =
        bearerTokenActor.currentToken()
}
