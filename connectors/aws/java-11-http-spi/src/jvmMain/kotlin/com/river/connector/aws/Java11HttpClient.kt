package com.river.connector.aws

import com.river.connector.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.future
import kotlinx.coroutines.reactive.asPublisher
import org.reactivestreams.FlowAdapters
import software.amazon.awssdk.http.SdkHttpResponse
import software.amazon.awssdk.http.async.AsyncExecuteRequest
import software.amazon.awssdk.http.async.SdkAsyncHttpClient
import software.amazon.awssdk.utils.AttributeMap
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpResponse
import java.nio.ByteBuffer
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Flow
import kotlin.jvm.optionals.getOrElse
import kotlin.jvm.optionals.getOrNull

/**
 * This class implements the SdkAsyncHttpClient interface using Java 11's HttpClient, allowing
 * it to be used with AWS SDK for Java v2. It provides asynchronous, non-blocking HTTP communication
 * with AWS services.
 *
 * Choosing this implementation over the Netty HTTP Client helps minimize additional dependencies and enhances
 * native JVM support.
 *
 * Java 11's HttpClient is highly optimized for handling reactive-streams,
 * making it an excellent alternative for an SdkAsyncHttpClient.
 *
 * You can keep using Netty's implementation by removing this `java-11-http-spi` dependency from the classpath.
 *
 * @property httpClient The Java 11 HttpClient instance to use for executing HTTP requests.
 * @property scope The CoroutineScope instance for executing coroutines.
 *
 */
class Java11HttpClient(
    private val httpClient: HttpClient,
    private val scope: CoroutineScope,
    private val attributes: AttributeMap
) : SdkAsyncHttpClient {
    companion object {
        private val headersToSkip = setOf("Host", "Content-Length", "Expect")

        fun builder(): Java11HttpClientBuilder =
            Java11HttpClientBuilder()

        fun buildDefault(): SdkAsyncHttpClient =
            builder().build()
    }

    override fun close() {
    }

    override fun execute(request: AsyncExecuteRequest): CompletableFuture<Void?> =
        scope.future {
            val handler = request.responseHandler()

            runCatching {
                val response = httpClient.coSend(
                    request = request(request.uri(), request.httpMethod()) {
                        expectContinue(request.expectContinue())
                        request.contentLength()?.also { publisherBody(request.body(), it) }
                        setHeaders(request.filteredHeaders())
                    },
                    bodyHandler = ofPublisher
                )

                handler.onHeaders(response.asSdkHttpResponse())
                handler.onStream(response.body())
            }.getOrElse {
                handler.onError(it)
            }

            null
        }

    private val ofPublisher = ofFlow.map { it.asPublisher() }

    private fun HttpResponse<*>.asSdkHttpResponse() =
        SdkHttpResponse
            .builder()
            .headers(headers().map())
            .statusCode(statusCode())
            .build()

    private fun AsyncExecuteRequest.uri(): URI =
        request().uri

    private fun AsyncExecuteRequest.httpMethod(): HttpMethod =
        HttpMethod.valueOf(request().method().name)

    private fun AsyncExecuteRequest.body(): Flow.Publisher<ByteBuffer> =
        FlowAdapters.toFlowPublisher(requestContentPublisher())

    private fun AsyncExecuteRequest.contentLength(): Long? =
        request()
            .firstMatchingHeader("Content-Length")
            .map { it.toLong() }
            .filter { it > 0 }
            .getOrNull()

    private fun AsyncExecuteRequest.filteredHeaders(): Map<String, List<String>> =
        request().headers().filterKeys { it !in headersToSkip }

    private fun AsyncExecuteRequest.expectContinue(): Boolean =
        request()
            .firstMatchingHeader("Expect")
            .map { "100-continue" == it }
            .getOrElse { false }
}
