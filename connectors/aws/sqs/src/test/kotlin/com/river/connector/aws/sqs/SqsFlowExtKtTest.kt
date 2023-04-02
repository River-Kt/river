package com.river.connector.aws.sqs

import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import com.river.connector.aws.sqs.model.Acknowledgment
import com.river.connector.aws.sqs.model.RequestMessage
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
    with(sqsClient) {
        feature("SQS as stream") {
            suspend fun purge() =
                listQueues()
                    .await()
                    .queueUrls()
                    .map { url -> purgeQueue { it.queueUrl(url) }.await() }

            val queue = createQueue { it.queueName("queue-test") }.await().queueUrl()

            suspend fun count() =
                getQueueAttributes { it.queueUrl(queue).attributeNames(QueueAttributeName.ALL) }
                    .await()
                    .attributes()[APPROXIMATE_NUMBER_OF_MESSAGES]
                    ?.toInt()

            scenario("Publish messages") {
                purge()

                (1..100)
                    .asFlow()
                    .map { RequestMessage("hello, $it!") }
                    .let { sendMessageFlow(queue, it) }
                    .collect()

                count() shouldBe 100
            }

            scenario("Receive messages") {
                purge()

                (1..100)
                    .asFlow()
                    .map { RequestMessage("hello, $it!") }
                    .let { sendMessageFlow(queue, it) }
                    .collect()

                val messages =
                    receiveMessagesFlow(stopOnEmptyList = true) {
                        queueUrl = queue
                        waitTimeSeconds = 0
                    }.toList()

                messages.size shouldBe 100

                messages.forEachIndexed { index, message ->
                    message.body() shouldBe "hello, ${index + 1}!"
                }
            }

            scenario("Commit messages") {
                purge()

                (1..100)
                    .asFlow()
                    .map { RequestMessage("hello, $it!") }
                    .let { sendMessageFlow(queue, it) }
                    .collect()

                val messages =
                    receiveMessagesFlow(stopOnEmptyList = true) {
                        queueUrl = queue
                        waitTimeSeconds = 0
                    }
                    .map { it.acknowledgeWith(Acknowledgment.Delete) }
                    .let { acknowledgmentMessageFlow(queue, it) }
                    .toList()

                messages.size shouldBe 100

                messages.forEach { message ->
                    message.acknowledgment shouldBe Acknowledgment.Delete
                }

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
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create("x", "x")
            )
        )
        .build()