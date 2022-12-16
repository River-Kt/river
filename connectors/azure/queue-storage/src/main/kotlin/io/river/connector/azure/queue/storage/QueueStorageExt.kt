package io.river.connector.azure.queue.storage

import com.azure.storage.queue.QueueAsyncClient
import com.azure.storage.queue.models.QueueMessageItem
import com.azure.storage.queue.models.SendMessageResult
import io.river.connector.azure.queue.storage.model.SendMessageRequest
import io.river.core.ParallelismIncreaseStrategy
import io.river.core.mapParallel
import io.river.core.unfoldParallel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

fun QueueAsyncClient.receiveMessagesAsFlow(
    maxParallelism: Int = 1,
    pollSize: Int = 32,
    visibilityTimeout: Duration = 30.seconds,
    stopOnEmptyList: Boolean = false,
    minimumParallelism: Int = 1,
    increaseStrategy: ParallelismIncreaseStrategy = ParallelismIncreaseStrategy.ByOne
): Flow<QueueMessageItem> =
    unfoldParallel(
        maxParallelism = maxParallelism,
        stopOnEmptyList = stopOnEmptyList,
        minimumParallelism = minimumParallelism,
        increaseStrategy = increaseStrategy
    ) {
        receiveMessages(pollSize, visibilityTimeout.toJavaDuration())
            .asFlow()
            .toList()
    }


fun QueueAsyncClient.deleteMessagesFlow(
    upstream: Flow<QueueMessageItem>,
    parallelism: Int = 100,
): Flow<Unit> =
    upstream
        .mapParallel(parallelism) {
            deleteMessage(it.messageId, it.popReceipt).awaitFirstOrNull()
            Unit
        }

fun QueueAsyncClient.sendMessagesFlow(
    upstream: Flow<SendMessageRequest>,
    parallelism: Int = 100,
): Flow<SendMessageResult> =
    upstream
        .mapParallel(parallelism) {
            sendMessageWithResponse(it.text, it.visibilityTimeout?.toJavaDuration(), it.ttl?.toJavaDuration())
                .awaitFirst()
                .value
        }
