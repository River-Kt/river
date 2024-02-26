@file:OptIn(ExperimentalRiverApi::class)

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.sqs.SqsClient
import aws.sdk.kotlin.services.sqs.model.SendMessageRequest
import aws.smithy.kotlin.runtime.net.url.Url
import com.github.tomakehurst.wiremock.WireMockServer
import com.river.connector.aws.sqs.model.SqsQueue
import com.river.connector.aws.sqs.sendMessageFlow
import com.river.core.ExperimentalRiverApi
import com.river.core.pollWithState
import com.river.core.throttle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlin.time.Duration

val sqs =
    SqsClient {
        endpointUrl = Url.parse("http://localhost:4566")
        region = "us-east-1"
        credentialsProvider = StaticCredentialsProvider {
            accessKeyId = "x"
            secretAccessKey = "x"
        }
    }

fun keepOnPublishing(
    queueUrl: String,
    elementsPerInternal: Int,
    interval: Duration
): Job {
    val requestFlow =
        pollWithState(0) { it + 1 to listOf(it) }
            .map { SendMessageRequest { messageBody = "$it" } }
            .throttle(elementsPerInternal, interval)

    return sqs
        .sendMessageFlow(SqsQueue.url(queueUrl), requestFlow)
        .launchIn(CoroutineScope(Dispatchers.Default))
}

inline fun withWiremock(f: (WireMockServer, Int) -> Unit) {
    val wiremock = WireMockServer()
    wiremock.start()
    f(wiremock, wiremock.port())
    wiremock.stop()
}

fun numbersApiUrl(port: Int) = "http://localhost:$port"
