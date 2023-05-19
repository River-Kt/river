package com.river.connector.http

import com.river.core.mapParallel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.future.await
import kotlinx.coroutines.jdk9.collect
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpResponse.*
import java.util.concurrent.Flow.Publisher

private val DefaultHttpClient: HttpClient = HttpClient.newHttpClient()

/**
 * Sends an HTTP request and returns the HTTP response.
 *
 * @param request The HTTP request to send.
 * @param bodyHandler The body handler to process the HTTP response body.
 *
 * @return The HTTP response.
 */
suspend fun <T> HttpClient.coSend(
    request: HttpRequest,
    bodyHandler: BodyHandler<T>
): HttpResponse<T> = sendAsync(request, bodyHandler).await()

/**
 * Sends an HTTP request and returns the HTTP response.
 *
 * @param request The HTTP request to send.
 * @param bodyHandler The body handler to process the HTTP response body.
 *
 * @return The HTTP response.
 */
suspend fun <T> HttpClient.coSend(
    bodyHandler: BodyHandler<T>,
    request: suspend () -> HttpRequest
): HttpResponse<T> = sendAsync(request(), bodyHandler).await()

/**
 * Sends this HTTP request and returns the HTTP response.
 *
 * @param bodyHandler The body handler to process the HTTP response body.
 * @param client The HTTP client used to send the request.
 *
 * @return The HTTP response.
 */
suspend fun <T> HttpRequest.coSend(
    bodyHandler: BodyHandler<T>,
    client: HttpClient = DefaultHttpClient
): HttpResponse<T> = client.coSend(this, bodyHandler)

/**
 * Converts the body of this HTTP response into a Flow.
 *
 * @return The body of the HTTP response as a Flow.
 */
fun <T> HttpResponse<Publisher<T>>.bodyAsFlow(): Flow<T> =
    flow { body().collect { emit(it) } }

/**
 * Sends each HTTP request in the flow and returns a flow of the HTTP responses.
 *
 * @param bodyHandler The body handler to process the HTTP response body.
 * @param parallelism The maximum number of concurrent requests.
 * @param httpClient The HTTP client used to send the requests.
 *
 * @return A flow of HTTP responses.
 */
fun <T> Flow<HttpRequest>.sendAndHandle(
    bodyHandler: BodyHandler<T>,
    parallelism: Int = 1,
    httpClient: HttpClient = DefaultHttpClient,
): Flow<HttpResponse<T>> =
    mapParallel(parallelism) { it.coSend(bodyHandler, httpClient) }

/**
 * Sends each HTTP request in the flow and returns a flow of the HTTP responses.
 *
 * @param parallelism The maximum number of concurrent requests.
 * @param httpClient The HTTP client used to send the requests.
 * @param handle A function that returns a body handler to process the HTTP response body.
 *
 * @return A flow of HTTP responses.
 */
fun <T> Flow<HttpRequest>.sendAndHandle(
    parallelism: Int = 1,
    httpClient: HttpClient = DefaultHttpClient,
    handle: CoroutineScope.() -> BodyHandler<T>,
): Flow<HttpResponse<T>> =
    flow {
        emitAll(
            coroutineScope { sendAndHandle(handle(), parallelism, httpClient) }
        )
    }
