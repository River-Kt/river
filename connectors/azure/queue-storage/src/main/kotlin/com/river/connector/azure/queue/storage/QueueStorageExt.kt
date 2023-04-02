package com.river.connector.azure.queue.storage

import com.azure.storage.queue.QueueAsyncClient
import com.azure.storage.queue.models.QueueMessageItem
import com.azure.storage.queue.models.SendMessageResult
import com.river.connector.azure.queue.storage.model.SendMessageRequest
import com.river.core.ParallelismIncreaseStrategy
import com.river.core.mapParallel
import com.river.core.unfoldParallel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

/**
 * Continuously receives messages from an Azure Storage Queue using the provided [QueueAsyncClient].
 * The received messages are returned as a [Flow] of [QueueMessageItem] objects.
 *
 * @param maxParallelism The maximum number of parallel receive operations. Defaults to 1.
 * @param pollSize The maximum number of messages to retrieve per poll request. Defaults to 32.
 * @param visibilityTimeout The visibility timeout for messages retrieved from the queue. Defaults to 30 seconds.
 * @param stopOnEmptyList If true, the flow will stop when an empty list of messages is received. Defaults to false.
 * @param minimumParallelism The minimum number of parallel receive operations. Defaults to 1.
 * @param increaseStrategy Determines how the parallelism increases when processing messages. Defaults to [ParallelismIncreaseStrategy.ByOne].
 *
 * @return A flow of QueueMessageItem objects.
 *
 * Example usage:
 *
 * ```
 *  val queueAsyncClient = QueueClientBuilder().queueName("name").buildAsyncClient()
 *  val messagesFlow = queueAsyncClient.receiveMessagesAsFlow()
 *  messagesFlow.collect { message -> println(message) }
 * ```
 */
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

/**
 * Deletes messages from an Azure Storage Queue using an upstream flow of QueueMessageItem objects.
 *
 * @param upstream The flow of QueueMessageItem objects to delete from the queue.
 * @param parallelism The parallelism for this operation. Defaults to 100.
 *
 * @return A flow of Unit objects.
 *
 * Example usage:
 *
 * ```
 *  val queueAsyncClient = QueueClientBuilder().queueName("name").buildAsyncClient()
 *  val messagesFlow = queueAsyncClient.receiveMessagesAsFlow()
 *  val deleteFlow = queueAsyncClient.deleteMessagesFlow(messagesFlow)
 *  deleteFlow.collect { println("Message deleted") }
 * ```
 */
fun QueueAsyncClient.deleteMessagesFlow(
    upstream: Flow<QueueMessageItem>,
    parallelism: Int = 100,
): Flow<Unit> =
    upstream
        .mapParallel(parallelism) {
            deleteMessage(it.messageId, it.popReceipt).awaitFirstOrNull()
            Unit
        }

/**
 * Sends messages to an Azure Storage Queue using an upstream flow of SendMessageRequest objects.
 *
 * @param upstream The flow of SendMessageRequest objects to send to the queue.
 * @param parallelism The parallelism for this operation. Defaults to 100.
 *
 * @return A flow of SendMessageResult objects.
 *
 * Example usage:
 *
 * ```
 *  val queueAsyncClient = QueueClientBuilder().queueName("name").buildAsyncClient()
 *  val messagesToSend = flowOf(SendMessageRequest("Hello, River!"))
 *  val sendFlow = queueAsyncClient.sendMessagesFlow(messagesToSend)
 *  sendFlow.collect { result -> println("Message sent: ${result.messageId}") }
 * ```
 */
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
