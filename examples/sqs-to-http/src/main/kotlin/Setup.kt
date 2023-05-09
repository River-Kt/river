import com.github.tomakehurst.wiremock.WireMockServer
import com.river.connector.aws.sqs.model.SendMessageRequest
import com.river.connector.aws.sqs.sendMessageFlow
import com.river.core.collectAsync
import com.river.core.pollWithState
import com.river.core.throttle
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.map
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import java.net.URI
import kotlin.time.Duration

val sqsAsyncClient: SqsAsyncClient =
    SqsAsyncClient
        .builder()
            .endpointOverride(URI("http://localhost:4566"))
            .region(Region.US_EAST_1)
            .credentialsProvider { AwsBasicCredentials.create("x", "x") }
        .build()

fun keepOnPublishing(
    queueUrl: String,
    elementsPerInternal: Int,
    interval: Duration
): Job {
    val requestFlow =
        pollWithState(0) { it + 1 to listOf(it) }
            .map { SendMessageRequest("$it") }
            .throttle(elementsPerInternal, interval)

    return sqsAsyncClient
        .sendMessageFlow(requestFlow) { queueUrl }
        .collectAsync()
}

inline fun withWiremock(f: (WireMockServer, Int) -> Unit) {
    val wiremock = WireMockServer()
    wiremock.start()
    f(wiremock, wiremock.port())
    wiremock.stop()
}

fun numbersApiUrl(port: Int) = "http://localhost:$port"
