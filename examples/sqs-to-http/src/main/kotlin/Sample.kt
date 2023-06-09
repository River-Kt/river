import com.marcinziolo.kotlin.wiremock.*
import com.river.connector.aws.sqs.acknowledgeWith
import com.river.connector.aws.sqs.acknowledgmentMessageFlow
import com.river.connector.aws.sqs.model.Acknowledgment
import com.river.connector.aws.sqs.receiveMessagesAsFlow
import com.river.connector.http.ofString
import com.river.connector.http.post
import com.river.connector.http.sendAndHandle
import com.river.core.ConcurrencyStrategy.Companion.increaseByOne
import com.river.core.alsoTo
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.future.await
import kotlin.time.Duration.Companion.milliseconds

suspend fun main() {
    val queueUrl = sqsAsyncClient.createQueue { it.queueName("numbers") }.await().queueUrl()

    // Publishing 10 events every 1/2 second to the queue just for the sake of this example
    keepOnPublishing(
        queueUrl = queueUrl,
        elementsPerInternal = 10,
        interval = 500.milliseconds
    )

    // Using Wiremock we define the API that is going to receive the requests.
    withWiremock { wiremock, port ->
        wiremock
            .post { url equalTo "/api/numbers" }
            .returns {
                // Adding a delay to make things fun
                delay = Wrapper(FixedDelay(300))
                body = "number registered successfully!"
            }

        // SQS consumer setup starts here
        sqsAsyncClient
            // receiveMessagesAsFlow continuously polls from SQS with a maximum of 5 concurrent requests
            // It starts with 1 concurrent request, and increases by 1 for each non-empty response, up to the maximum
            // If the queue returns an empty response, the consumer resets concurrency until data is returned again
            .receiveMessagesAsFlow(increaseByOne(5)) {
                // The receiver type here is a ReceiveMessageRequestBuilder
                // It allows us to define the specific requests we want to make to SQS
                // Here, we're only setting the queue URL
                this.queueUrl = queueUrl
            }
            // alsoTo performs a side operation with a Flow, allowing us to keep the Message instance for later deletion
            .alsoTo {
                // Create a POST request using the SQS message body
                map { post("${numbersApiUrl(port)}/api/numbers") { stringBody(it.body()) } }
                    // Send the request to the API with a maximum concurrency of 25
                    .sendAndHandle(bodyHandler = ofString, concurrency = 25)
                    .takeWhile { it.statusCode() == 200 }
            }
            .map { (message, _) ->
                message.acknowledgeWith(Acknowledgment.Delete)
            }
            // And now we delete the message in SQS
            // It's possible to customize the message deletion concurrency as well
            .let { sqsAsyncClient.acknowledgmentMessageFlow(it, 10) { queueUrl } }
            .collect(::println) // Collect is required to initiate the process
    }
}
