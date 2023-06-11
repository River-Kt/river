package com.river.connector.google.drive

import com.fasterxml.jackson.module.kotlin.readValue
import com.river.connector.format.json.defaultObjectMapper
import com.river.connector.http.coSend
import com.river.connector.http.get
import com.river.connector.http.ofFlow
import com.river.connector.http.ofString
import com.river.core.asByteArray
import com.river.core.pollWithState
import com.river.core.stoppableFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll

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
fun GoogleDriveApi.listFiles(
    query: FilesListQuery.() -> Unit = {}
): Flow<GoogleDriveFile> =
    FilesListQuery().also(query).let { filter ->
        pollWithState(
            initial = true to null as String?,
            shouldStop = { (first, nextToken) -> nextToken == null || first }
        ) { (_, token) ->
            filter.pageToken = token

            val request = get("$baseUrl/v3/files") {
                authorization { bearer(currentToken()) }

                filter
                    .build()
                    .filterValues { it != null }
                    .forEach { query(it.key, "${it.value}") }
            }

            val body: String = httpClient.coSend(request, ofString).body()
            val response = defaultObjectMapper.readValue<FileList>(body)

            (false to response.nextPageToken) to response.files
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
 * val serviceAccount: JsonNode = // provide your credentials here
 * val googleDriveApi = GoogleDriveApi(serviceAccount)
 * val fileDataFlow: Flow<ByteArray> = googleDriveApi.download("0BwwA4oUTeiV1UVNwOHItT0xfa2M")
 *
 * fileDataFlow.collect { fileData ->
 *     println(fileData.size)
 * }
 * ```
 */
fun GoogleDriveApi.download(
    fileId: String
): Flow<ByteArray> = stoppableFlow {
    val accessToken = currentToken()

    val request = get("$baseUrl/v3/files/$fileId") {
        query("fields", "webContentLink")
        authorization { bearer(accessToken) }
    }

    val response = httpClient.coSend(request, ofString)

    require(response.statusCode() != 401) { "The provided authorization token is not valid." }
    require(response.statusCode() != 404) { "The file $fileId does not exist." }
    require(response.statusCode() == 200) { "Received HTTP status: ${response.statusCode()}" }

    val jsonBody = defaultObjectMapper.readTree(response.body())

    val url = jsonBody["webContentLink"].asText()

    httpClient
        .coSend(
            request = get(url) {
                authorization { bearer(accessToken) }
            },
            bodyHandler = ofFlow
        )
        .body()
        .asByteArray()
        .also {
            emitAll(it)
        }
}
