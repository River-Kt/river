package com.river.connector.aws.sqs

import com.river.connector.aws.sqs.model.Acknowledgment
import com.river.connector.aws.sqs.model.SendMessageRequest
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.future.await
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.QueueAttributeName
import software.amazon.awssdk.services.sqs.model.QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES
import java.net.URI

class SqsFlowExtKtTest : FeatureSpec({
    val queueName = "queue-test"

    with(sqsClient) {
        suspend fun purge() =
            listQueues()
                .await()
                .queueUrls()
                .map { url -> purgeQueue { it.queueUrl(url) }.await() }

        suspend fun queue() = createQueue { it.queueName(queueName) }.await().queueUrl()

        suspend fun count() =
            queue().let { url ->
                getQueueAttributes { it.queueUrl(url).attributeNames(QueueAttributeName.ALL) }
                    .await()
                    .attributes()[APPROXIMATE_NUMBER_OF_MESSAGES]
                    ?.toInt()
            }

        beforeTest {
            purge()
        }

        feature("SQS as stream") {
            scenario("Publish messages") {
                (1..100)
                    .asFlow()
                    .map { SendMessageRequest("hello, $it!") }
                    .let { sendMessageFlow(it) { queue() } }
                    .collect()

                count() shouldBe 100
            }

            scenario("Receive messages") {
                (1..100)
                    .asFlow()
                    .map { SendMessageRequest("hello, $it!") }
                    .let { sendMessageFlow(it) { queue() } }
                    .collect()

                val messages =
                    receiveMessagesAsFlow(stopOnEmptyList = true) {
                        queueUrl = queue()
                        waitTimeSeconds = 0
                    }.toList()

                messages.size shouldBe 100

                messages.forEachIndexed { index, message ->
                    message.body() shouldBe "hello, ${index + 1}!"
                }
            }

            scenario("Commit messages") {
                (1..100)
                    .asFlow()
                    .map { SendMessageRequest("hello, $it!") }
                    .let { sendMessageFlow(it) { queue() } }
                    .collect()

                val messages =
                    receiveMessagesAsFlow(stopOnEmptyList = true) {
                        queueUrl = queue()
                        waitTimeSeconds = 0
                    }.map { it.acknowledgeWith(Acknowledgment.Delete) }
                        .let { acknowledgmentMessageFlow(it) { queue() } }
                        .toList()

                messages.size shouldBe 100

                messages.forEach { message ->
                    message.acknowledgment shouldBe Acknowledgment.Delete
                }

                count() shouldBe 0
            }
        }

        feature("SQS consumer") {
            scenario("client.onMessage should consume messages and acknowledge them properly") {
                (1..100)
                    .asFlow()
                    .map { SendMessageRequest("hello, $it!") }
                    .let { sendMessageFlow(it) { queue() } }
                    .collect()

                count() shouldBe 100

                val consumerJob =
                    onMessage(
                        queueName = queueName,
                        receiveConfiguration = {
                            stopOnEmptyList = true

                            receiveRequest { waitTimeSeconds = 0 }
                        }
                    ) {
                        it.acknowledgeWith(Acknowledgment.Delete)
                    }

                consumerJob.join()
                count() shouldBe 0
            }
        }
    }
})

val sqsClient: SqsAsyncClient =
    SqsAsyncClient
        .builder()
        .endpointOverride(URI("http://localhost:4566"))
        .region(Region.US_EAST_1)
        .credentialsProvider { AwsBasicCredentials.create("x", "x") }
        .build()
