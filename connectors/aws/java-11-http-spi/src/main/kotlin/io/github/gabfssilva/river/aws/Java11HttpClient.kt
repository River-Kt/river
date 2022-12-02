package io.github.gabfssilva.river.aws

import io.github.gabfssilva.river.util.http.method
import io.github.gabfssilva.river.util.http.ofFlow
import io.github.gabfssilva.river.util.http.send
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.future.future
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.asPublisher
import org.reactivestreams.FlowAdapters
import software.amazon.awssdk.http.SdkHttpResponse
import software.amazon.awssdk.http.async.AsyncExecuteRequest
import software.amazon.awssdk.http.async.SdkAsyncHttpClient
import java.net.http.HttpClient
import java.util.concurrent.CompletableFuture

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
                    method(awsReq.method().name, awsReq.uri.toString()) {
                        headers.putAll(awsReq.headers().filterKeys { it !in headersToSkip })
                        expectContinue = awsReq.headers()["Expect"]?.firstOrNull()?.equals("100-continue") ?: false

                        body(
                            body = FlowAdapters.toFlowPublisher(asyncExecuteRequest.requestContentPublisher()),
                            contentLength = awsReq.headers()["Content-Length"]?.firstOrNull()?.toLong() ?: 0
                        )
                    }

                val response = request.send(ofFlow, httpClient)

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
