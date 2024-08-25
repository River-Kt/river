package com.river.connector.google.drive

import com.river.core.ExperimentalRiverApi
import com.river.core.pollWithState

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * This function allows for listing files on Google Drive with an optional query to filter the files.
 *
 * @param query A lambda with receiver on `FilesListQuery`. Allows to construct a query for filtering files. Default value is an empty lambda.
 * @return A Flow of `GoogleDriveFile`. Each `GoogleDriveFile` is a file that matches the specified query on Google Drive.
 *
 * Example usage:
 *
 * ```
 * val serviceAccount: JsonNode = // provide your credentials here
 * val googleDriveApi = GoogleDriveApi(serviceAccount)
 *
 * val filesFlow: Flow<GoogleDriveFile> = googleDriveApi.listFiles {
 *     driveId = "x"
 * }
 *
 * filesFlow.collect { file ->
 *     println(file.name)
 * }
 * ```
 */
@ExperimentalRiverApi
fun GoogleDriveApi.listFiles(
    query: FilesListQuery.() -> Unit = {}
): Flow<GoogleDriveFile> =
    FilesListQuery().also(query).let { filter ->
        pollWithState(
            initial = true to null as String?,
            shouldStop = { (first, nextToken) -> nextToken == null || first }
        ) { (_, token) ->
            filter.pageToken = token

            val response = httpClient.get("$baseUrl/v3/files") {
                bearerAuth(currentToken())

                url {
                    filter
                        .build()
                        .filterValues { it != null }
                        .forEach { parameters.append(it.key, "${it.value}") }
                }
            }

            val body = response.body<FileList>()
            (false to body.nextPageToken) to body.files
        }
    }

/**
 * This function allows for downloading a file from Google Drive.
 *
 * @param fileId The ID of the file to be downloaded.
 * @return A Flow of ByteArray representing the file data.
 *
 * Example usage:
 *
 * ```
 * val serviceAccount: JsonObject = // provide your credentials here
 * val googleDriveApi = GoogleDriveApi(serviceAccount)
 * val fileDataFlow: Flow<ByteArray> = googleDriveApi.download("0BwwA4oUTeiV1UVNwOHItT0xfa2M")
 *
 * fileDataFlow.collect { fileData ->
 *     println(fileData.size)
 * }
 * ```
 */
@ExperimentalRiverApi
fun GoogleDriveApi.download(
    fileId: String
): Flow<ByteArray> = channelFlow {
    val accessToken = currentToken()

    val response = httpClient.get("$baseUrl/v3/files/$fileId") {
        url { parameters.append("fields", "webContentLink") }
        bearerAuth(accessToken)
    }

    require(response.status.value != 401) { "The provided authorization token is not valid." }
    require(response.status.value != 404) { "The file $fileId does not exist." }
    require(response.status.value == 200) { "Received HTTP status: ${response.status}" }

    val jsonBody = response.body<JsonObject>()

    val url = checkNotNull(jsonBody["webContentLink"]?.jsonPrimitive?.content)

    httpClient
        .prepareGet(url) { bearerAuth(accessToken) }
        .execute {
            val content = it.bodyAsChannel()

            while (!content.isClosedForRead) {
                val packet = content.readRemaining(4096)
                while (!packet.isEmpty) send(packet.readBytes())
            }
        }
}
