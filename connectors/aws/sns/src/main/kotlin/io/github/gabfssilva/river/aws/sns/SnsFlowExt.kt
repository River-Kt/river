package io.github.gabfssilva.river.aws.sns

import io.github.gabfssilva.river.core.ChunkStrategy
import io.github.gabfssilva.river.core.chunked
import io.github.gabfssilva.river.core.mapParallel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.future.await
import software.amazon.awssdk.services.sns.SnsAsyncClient
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishBatchRequestEntry
import java.util.*
import kotlin.time.Duration.Companion.milliseconds

context(Flow<PublishBatchRequestEntry>)
fun SnsAsyncClient.publishFlow(
    topicArn: String,
    parallelism: Int = 1,
    chunkStrategy: ChunkStrategy = ChunkStrategy.TimeWindow(10, 250.milliseconds)
) = chunked(chunkStrategy)
    .mapParallel(parallelism) {
        publishBatch { builder ->
            builder
                .publishBatchRequestEntries(it)
                .topicArn(topicArn)
        }.await()
    }

fun SnsAsyncClient.publishFlow(
    topicArn: String,
    upstream: Flow<PublishBatchRequestEntry>,
    parallelism: Int = 1,
    chunkStrategy: ChunkStrategy = ChunkStrategy.TimeWindow(10, 250.milliseconds)
) = with(upstream) { publishFlow(topicArn, parallelism, chunkStrategy) }

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
