package com.river.connector.aws

import com.river.connector.http.HttpMethod
import com.river.connector.http.coSend
import com.river.connector.http.ofFlow
import com.river.connector.http.request
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.future.future
import kotlinx.coroutines.reactive.asPublisher
import org.reactivestreams.FlowAdapters
import software.amazon.awssdk.http.SdkHttpResponse
import software.amazon.awssdk.http.async.AsyncExecuteRequest
import software.amazon.awssdk.http.async.SdkAsyncHttpClient
import java.net.http.HttpClient
import java.util.concurrent.CompletableFuture

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
    private val scope: CoroutineScope
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

    override fun execute(asyncExecuteRequest: AsyncExecuteRequest): CompletableFuture<Void?> =
        scope.future {
            val handler = asyncExecuteRequest.responseHandler()

            runCatching {
                val awsReq = asyncExecuteRequest.request()

                val request =
                    request(awsReq.uri.toString(), HttpMethod.valueOf(awsReq.method().name)) {
                        setHeaders(awsReq.headers().filterKeys { it !in headersToSkip })
                        expectContinue(awsReq.headers()["Expect"]?.firstOrNull()?.equals("100-continue") ?: false)

                        publisherBody(
                            body = FlowAdapters.toFlowPublisher(asyncExecuteRequest.requestContentPublisher()),
                            contentLength = awsReq.headers()["Content-Length"]?.firstOrNull()?.toLong() ?: 0
                        )
                    }

                val response = request.coSend(ofFlow, httpClient)

                val awsHeaders = SdkHttpResponse
                    .builder()
                    .headers(response.headers().map())
                    .statusCode(response.statusCode())
                    .build()

                handler.onHeaders(awsHeaders)

                handler.onStream(
                    response
                        .body()
                        .catch { handler.onError(it); throw it }
                        .asPublisher()
                )
            }.getOrElse {
                handler.onError(it)
            }

            null
        }
}
