package com.river.connector.aws.sqs

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.sqs.SqsClient
import aws.sdk.kotlin.services.sqs.createQueue
import aws.sdk.kotlin.services.sqs.getQueueAttributes
import aws.sdk.kotlin.services.sqs.model.QueueAttributeName
import aws.sdk.kotlin.services.sqs.model.SendMessageRequest
import aws.sdk.kotlin.services.sqs.purgeQueue
import aws.smithy.kotlin.runtime.net.url.Url
import com.river.connector.aws.sqs.model.Acknowledgment
import com.river.connector.aws.sqs.model.SqsQueue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList

class SqsFlowExtKtTest : FunSpec({
    val queue = SqsQueue { createQueue { queueName = "queue-test" }.queueUrl.orEmpty() }

    with(sqsClient) {
        suspend fun count(): Int =
            queue
                .url()
                .let { url ->
                    getQueueAttributes {
                        queueUrl = url
                        attributeNames = listOf(QueueAttributeName.All)
                    }
                }
                .attributes
                ?.get("APPROXIMATE_NUMBER_OF_MESSAGES")
                ?.toInt() ?: -1

        beforeTest {
            val url = queue.url()
            purgeQueue { queueUrl = url }
        }

        test("Publish messages to SQS") {
            (1..100)
                .asFlow()
                .map { SendMessageRequest { messageBody = "hello, #$it!" } }
                .let { sendMessageFlow(queue, it) }
                .collect()

            count() shouldBe 100
        }

        test("Receive messages from SQS") {
            (1..100)
                .asFlow()
                .map { SendMessageRequest { messageBody = "hello, #$it!" } }
                .let { sendMessageFlow(queue, it) }
                .collect()

            val queueUrl = queue.url()

            val messages =
                receiveMessagesAsFlow(stopOnEmptyList = true) {
                    this.queueUrl = queueUrl
                    waitTimeSeconds = 0
                }.toList()

            messages.size shouldBe 100

            messages.forEachIndexed { index, message ->
                message.body shouldBe "hello, #${index + 1}!"
            }
        }
//
//        feature("SQS consumer") {
//            scenario("client.onMessage should consume messages and acknowledge them properly") {
//                (1..100)
//                    .asFlow()
//                    .map { SendMessageRequest("hello, $it!") }
//                    .let { sendMessageFlow(it) { queue() } }
//                    .collect()
//
//                count() shouldBe 100
//
//                val consumerJob =
//                    onMessage(
//                        queueName = queueName,
//                        receiveConfiguration = {
//                            stopOnEmptyList = true
//
//                            receiveRequest { waitTimeSeconds = 0 }
//                        }
//                    ) {
//                        Acknowledgment.Delete
//                    }
//
//                consumerJob.join()
//                count() shouldBe 0
//            }
//        }
    }
})

val sqsClient: SqsClient =
    SqsClient {
        endpointUrl = Url.parse("http://localhost:4566")
        region = "us-east-1"
        credentialsProvider = StaticCredentialsProvider {
            accessKeyId = "x"
            secretAccessKey = "x"
        }
    }