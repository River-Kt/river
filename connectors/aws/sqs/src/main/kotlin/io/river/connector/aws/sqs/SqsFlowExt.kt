package io.river.connector.aws.sqs

import io.river.core.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.future.await
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import software.amazon.awssdk.core.SdkResponse
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.*
import java.util.*
import kotlin.time.Duration.Companion.milliseconds

val SqsAsyncClient.logger: Logger
    get() = LoggerFactory.getLogger(javaClass)

fun Message.acknowledgeWith(acknowledgment: Acknowledgment) =
    MessageAcknowledgment(this, acknowledgment)

suspend fun SqsAsyncClient.getQueueUrl(name: String): String =
    getQueueUrl { it.queueName(name) }
        .await()
        .queueUrl()

inline fun SqsAsyncClient.receiveMessagesFlow(
    maxParallelism: Int = 1,
    stopOnEmptyList: Boolean = false,
    minimumParallelism: Int = 1,
    increaseStrategy: ParallelismIncreaseStrategy = ParallelismIncreaseStrategy.ByOne,
    builder: ReceiveMessageRequestBuilder.() -> Unit
): Flow<Message> =
    ReceiveMessageRequestBuilder()
        .also(builder)
        .build()
        .let { request ->
            unfoldParallel(
                maxParallelism = maxParallelism,
                stopOnEmptyList = stopOnEmptyList,
                minimumParallelism = minimumParallelism,
                increaseStrategy = increaseStrategy
            ) {
                receiveMessage(request)
                    .await()
                    .messages()
            }
        }

context(Flow<MessageAcknowledgment<Acknowledgment.Delete>>)
fun SqsAsyncClient.deleteMessagesFlow(
    queueUrl: String,
    parallelism: Int = 1,
    chunkStrategy: ChunkStrategy = ChunkStrategy.TimeWindow(10, 250.milliseconds)
) = chunked(chunkStrategy)
    .mapParallel(parallelism) { messages ->
        deleteMessageBatch {
            logger.info("Sending ${messages.size} messages to queue")

            it.queueUrl(queueUrl)

            it.entries(
                messages
                    .mapIndexed { index, result ->
                        DeleteMessageBatchRequestEntry
                            .builder()
                            .receiptHandle(result.message.receiptHandle())
                            .id("$index")
                            .build()
                    }
            )
        }
        .await()
        .let { response -> messages.map { it to response } }
    }
    .flatten()

context(Flow<MessageAcknowledgment<Acknowledgment.ChangeMessageVisibility>>)
fun SqsAsyncClient.changeMessageVisibilityFlow(
    queueUrl: String,
    parallelism: Int = 1,
    chunkStrategy: ChunkStrategy = ChunkStrategy.TimeWindow(10, 250.milliseconds)
) = chunked(chunkStrategy)
    .mapParallel(parallelism) { messages ->
        changeMessageVisibilityBatch {
            it.queueUrl(queueUrl)

            it.entries(
                messages
                    .mapIndexed { index, message ->
                        ChangeMessageVisibilityBatchRequestEntry
                            .builder()
                            .visibilityTimeout(message.acknowledgment.timeout)
                            .receiptHandle(message.message.receiptHandle())
                            .id("$index")
                            .build()
                    }
            )
        }
        .await()
        .let { response -> messages.map { it to response } }
    }
    .flatten()

fun MessageRequestEntry(
    body: String,
    delaySeconds: Int = 0,
    messageAttributes: Map<String, MessageAttributeValue> = emptyMap(),
    id: String = UUID.randomUUID().toString()
): SendMessageBatchRequestEntry =
    SendMessageBatchRequestEntry
        .builder()
        .apply {
            messageBody(body)
            delaySeconds(delaySeconds)
            messageAttributes(messageAttributes)
            id(id)
        }
        .build()

fun SqsAsyncClient.sendMessageFlow(
    upstream: Flow<SendMessageBatchRequestEntry>,
    queueUrl: String,
    parallelism: Int = 1,
    chunkStrategy: ChunkStrategy = ChunkStrategy.TimeWindow(10, 250.milliseconds)
) = with(upstream) { sendMessageFlow(queueUrl, parallelism, chunkStrategy) }

context(Flow<SendMessageBatchRequestEntry>)
fun SqsAsyncClient.sendMessageFlow(
    queueUrl: String,
    parallelism: Int = 1,
    chunkStrategy: ChunkStrategy = ChunkStrategy.TimeWindow(10, 250.milliseconds)
) = chunked(chunkStrategy)
    .mapParallel(parallelism) { entries ->
        sendMessageBatch {
            it.queueUrl(queueUrl)
                .entries(entries)
        }.await()
    }

fun SqsAsyncClient.changeMessageVisibilityFlow(
    upstream: Flow<MessageAcknowledgment<Acknowledgment.ChangeMessageVisibility>>,
    queueUrl: String,
    parallelism: Int = 1,
    chunkStrategy: ChunkStrategy = ChunkStrategy.TimeWindow(10, 250.milliseconds)
): Flow<Pair<MessageAcknowledgment<Acknowledgment.ChangeMessageVisibility>, ChangeMessageVisibilityBatchResponse>> {
    val client = this

    with(upstream) {
        return client.changeMessageVisibilityFlow(queueUrl, parallelism, chunkStrategy)
    }
}

fun SqsAsyncClient.deleteMessagesFlow(
    upstream: Flow<MessageAcknowledgment<Acknowledgment.Delete>>,
    queueUrl: String,
    parallelism: Int = 1,
    chunkStrategy: ChunkStrategy = ChunkStrategy.TimeWindow(10, 250.milliseconds)
): Flow<Pair<MessageAcknowledgment<Acknowledgment.Delete>, DeleteMessageBatchResponse>> {
    val client = this

    with(upstream) {
        return client.deleteMessagesFlow(queueUrl, parallelism, chunkStrategy)
    }
}

context(Flow<MessageAcknowledgment<*>>)
fun SqsAsyncClient.acknowledgmentMessageFlow(
    queueUrl: String,
    parallelism: Int = 1,
    chunkStrategy: ChunkStrategy = ChunkStrategy.TimeWindow(10, 250.milliseconds)
): Flow<AcknowledgmentResult<SdkResponse>> {
    val deleteMessageChannel = Channel<MessageAcknowledgment<Acknowledgment.Delete>>()
    val changeMessageVisibilityChannel = Channel<MessageAcknowledgment<Acknowledgment.ChangeMessageVisibility>>()
    val ignoreChannel = Channel<MessageAcknowledgment<Acknowledgment.Ignore>>()

    val deleteFlow =
        deleteMessagesFlow(
            deleteMessageChannel.receiveAsFlow(),
            queueUrl,
            parallelism,
            chunkStrategy
        )

    val changeVisibilityFlow =
        changeMessageVisibilityFlow(
            changeMessageVisibilityChannel.receiveAsFlow(),
            queueUrl,
            parallelism,
            chunkStrategy
        )

    val ignoreFlow =
        ignoreChannel
            .receiveAsFlow()
            .map { it to null }

    onCompletion {
        deleteMessageChannel.close()
        ignoreChannel.close()
        changeMessageVisibilityChannel.close()
    }.collectAsync {
        when (it.acknowledgment) {
            is Acknowledgment.ChangeMessageVisibility ->
                changeMessageVisibilityChannel.send(it as MessageAcknowledgment<Acknowledgment.ChangeMessageVisibility>)

            Acknowledgment.Delete ->
                deleteMessageChannel.send(it as MessageAcknowledgment<Acknowledgment.Delete>)

            Acknowledgment.Ignore ->
                ignoreChannel.send(it as MessageAcknowledgment<Acknowledgment.Ignore>)
        }
    }

    return merge(deleteFlow, changeVisibilityFlow, ignoreFlow)
        .map { (ack, response) -> AcknowledgmentResult(ack.message, ack.acknowledgment, response) }
}
