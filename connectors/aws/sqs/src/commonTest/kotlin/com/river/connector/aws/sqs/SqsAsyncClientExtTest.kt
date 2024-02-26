@file:OptIn(InternalApi::class)

package com.river.connector.aws.sqs

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.sqs.SqsClient
import aws.sdk.kotlin.services.sqs.createQueue
import aws.sdk.kotlin.services.sqs.getQueueAttributes
import aws.sdk.kotlin.services.sqs.model.QueueAttributeName
import aws.sdk.kotlin.services.sqs.model.SendMessageRequest
import aws.sdk.kotlin.services.sqs.purgeQueue
import aws.smithy.kotlin.runtime.InternalApi
import aws.smithy.kotlin.runtime.net.url.Url
import aws.smithy.kotlin.runtime.util.PlatformProvider
import com.river.connector.aws.sqs.model.Acknowledgment
import com.river.connector.aws.sqs.model.SqsQueue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkObject
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList

@OptIn(InternalApi::class)
class SqsFlowExtKtTest : FunSpec({
    val queue = SqsQueue { createQueue { queueName = "queue-test" }.queueUrl.orEmpty() }

    context("Amazon Simple Queue Service as stream ${PlatformProvider.System}") {
        val url = with(sqsClient) { queue.url() }

        beforeTest { sqsClient.purgeQueue { queueUrl = url } }

        test("Send messages to queue should work as expected") {
            (1..100)
                .asFlow()
                .map { SendMessageRequest { messageBody = "hello, #$it!" } }
                .let { sqsClient.sendMessageFlow(queue, it) }
                .collect()

            queue.count() shouldBe 100
        }

        test("Receiving messages from queue should work as expected") {
            (1..100)
                .asFlow()
                .map { SendMessageRequest { messageBody = "hello, #$it!" } }
                .let { sqsClient.sendMessageFlow(queue, it) }
                .collect()

            queue.count() shouldBe 100

            val messages =
                sqsClient.receiveMessagesAsFlow(stopOnEmptyList = true) {
                    queueUrl = url
                    waitTimeSeconds = 0
                }.toList()

            messages.size shouldBe 100

            messages.forEachIndexed { index, message ->
                message.body shouldBe "hello, #${index + 1}!"
            }
        }

        test(".onMessages should work as expected") {
            (1..100)
                .asFlow()
                .map { SendMessageRequest { messageBody = "hello, #$it!" } }
                .let { sqsClient.sendMessageFlow(queue, it) }
                .collect()

            queue.count() shouldBe 100

            val consumerJob =
                sqsClient
                    .onMessage(
                        queue = queue,
                        receiveConfiguration = {
                            stopOnEmptyList = true
                            receiveRequest { waitTimeSeconds = 0 }
                        }
                    ) {
                        Acknowledgment.Delete
                    }

            consumerJob.join()
            queue.count() shouldBe 0
        }
    }
})

val sqsClient: SqsClient by lazy {
    lateinit var client: SqsClient

    mockkObject(PlatformProvider.System) {
        every { PlatformProvider.System.isAndroid } returns false

        client = SqsClient {
            endpointUrl = Url.parse("http://localhost:4566")
            region = "us-east-1"
            credentialsProvider = StaticCredentialsProvider {
                accessKeyId = "x"
                secretAccessKey = "x"
            }
            applicationId = "x"
        }
    }

    client
}

suspend fun SqsQueue.count(): Int =
    with(sqsClient) {
        url()
            .let { url ->
                getQueueAttributes {
                    queueUrl = url
                    attributeNames = listOf(QueueAttributeName.All)
                }
            }
            .attributes
            ?.get(QueueAttributeName.ApproximateNumberOfMessages.value)
            ?.toInt() ?: -1
    }
