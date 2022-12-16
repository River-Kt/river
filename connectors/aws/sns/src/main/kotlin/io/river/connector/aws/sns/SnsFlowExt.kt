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

context(Flow<PublishBatchRequestEntry>)
fun SnsAsyncClient.publishFlow(
    topicArn: String,
    parallelism: Int = 1,
    chunkStrategy: ChunkStrategy = ChunkStrategy.TimeWindow(10, 250.milliseconds)
): Flow<PublishBatchResponse> =
    chunked(chunkStrategy)
        .mapParallel(parallelism) {
            publishBatch { builder -> builder.publishBatchRequestEntries(it).topicArn(topicArn) }
                .await()
        }

fun SnsAsyncClient.publishFlow(
    topicArn: String,
    upstream: Flow<PublishBatchRequestEntry>,
    parallelism: Int = 1,
    chunkStrategy: ChunkStrategy = ChunkStrategy.TimeWindow(10, 250.milliseconds)
): Flow<PublishBatchResponse> =
    with(upstream) { publishFlow(topicArn, parallelism, chunkStrategy) }

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
