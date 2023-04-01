package io.river.connector.aws.sns

import io.river.core.ChunkStrategy
import io.river.core.chunked
import io.river.core.mapParallel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.future.await
import software.amazon.awssdk.services.sns.SnsAsyncClient
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishBatchRequestEntry
import software.amazon.awssdk.services.sns.model.PublishBatchResponse
import java.util.*
import kotlin.time.Duration.Companion.milliseconds

/**
 * Creates a flow that publishes messages to an Amazon SNS topic.
 *
 * This function takes an [upstream] flow of [PublishBatchRequestEntry] objects and publishes
 * them to an SNS topic specified by [topicArn]. The function processes messages in parallel
 * using [parallelism] and the specified [chunkStrategy].
 *
 * @param topicArn The ARN of the SNS topic.
 * @param upstream A [Flow] of [PublishBatchRequestEntry] objects.
 * @param parallelism The level of parallelism for processing messages.
 * @param chunkStrategy The strategy to use when chunking messages for processing.
 * @return A [Flow] of [PublishBatchResponse] objects.
 *
 * Example usage:
 *
 * ```
 * val snsClient: SnsAsyncClient = ...
 * val topicArn = "arn:aws:sns:us-east-1:123456789012:mytopic"
 * val messages = flowOf(
 *     PublishRequestEntry("Hello, world!"),
 *     PublishRequestEntry("Another message")
 * )
 *
 * snsClient.publishFlow(topicArn, messages)
 *     .collect { response ->
 *         println("Published messages: $response")
 *     }
 * ```
 */
fun SnsAsyncClient.publishFlow(
    topicArn: String,
    upstream: Flow<PublishBatchRequestEntry>,
    parallelism: Int = 1,
    chunkStrategy: ChunkStrategy = ChunkStrategy.TimeWindow(10, 250.milliseconds)
): Flow<PublishBatchResponse> =
    upstream
        .chunked(chunkStrategy)
        .mapParallel(parallelism) {
            publishBatch { builder -> builder.publishBatchRequestEntries(it).topicArn(topicArn) }
                .await()
        }

/**
 * Creates a [PublishBatchRequestEntry] object for sending messages to an Amazon SNS topic.
 *
 * @param message The content of the message to send.
 * @param messageAttributes A map of [MessageAttributeValue] objects for the message.
 * @param id A unique identifier for the message (optional, default is a random UUID).
 * @return A [PublishBatchRequestEntry] object.
 *
 * Example usage:
 *
 * ```
 * val message1 = PublishRequestEntry("Hello, world!")
 * val message2 = PublishRequestEntry(
 *     "Another message",
 *     messageAttributes = mapOf("attributeKey" to MessageAttributeValue.builder().stringValue("attributeValue").dataType("String").build())
 * )
 * val messages = flowOf(message1, message2)
 * ```
 */
fun PublishRequestEntry(
    message: String,
    messageAttributes: Map<String, MessageAttributeValue> = emptyMap(),
    id: String = UUID.randomUUID().toString()
): PublishBatchRequestEntry =
    PublishBatchRequestEntry
        .builder()
        .apply {
            message(message)
            messageAttributes(messageAttributes)
            id(id)
        }
        .build()
