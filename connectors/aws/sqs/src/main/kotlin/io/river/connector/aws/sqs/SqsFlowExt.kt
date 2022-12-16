package io.river.connector.aws.sqs

import io.river.connector.aws.sqs.model.*
import io.river.connector.aws.sqs.model.Acknowledgment.*
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

internal val SqsAsyncClient.logger: Logger
    get() = LoggerFactory.getLogger(javaClass)

fun SqsAsyncClient.receiveMessagesFlow(
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

fun SqsAsyncClient.changeMessageVisibilityFlow(
    queueUrl: String,
    upstream: Flow<MessageAcknowledgment<ChangeMessageVisibility>>,
    parallelism: Int = 1,
    chunkStrategy: ChunkStrategy = ChunkStrategy.TimeWindow(10, 250.milliseconds)
) = upstream
    .chunked(chunkStrategy)
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
        }.await().let { response -> messages.map { it to response } }
    }
    .flatten()

fun SqsAsyncClient.sendMessageFlow(
    queueUrl: String,
    upstream: Flow<RequestMessage>,
    parallelism: Int = 1,
    chunkStrategy: ChunkStrategy = ChunkStrategy.TimeWindow(10, 250.milliseconds)
) = upstream
    .map { it.asMessageRequestEntry() }
    .chunked(chunkStrategy)
    .mapParallel(parallelism) { entries ->
        sendMessageBatch { it.queueUrl(queueUrl).entries(entries) }
            .await()
    }

fun SqsAsyncClient.deleteMessagesFlow(
    queueUrl: String,
    upstream: Flow<MessageAcknowledgment<Delete>>,
    parallelism: Int = 1,
    chunkStrategy: ChunkStrategy = ChunkStrategy.TimeWindow(10, 250.milliseconds)
): Flow<Pair<MessageAcknowledgment<Delete>, DeleteMessageBatchResponse>> =
    upstream
        .chunked(chunkStrategy)
        .mapParallel(parallelism) { messages ->
            deleteMessageBatch {
                logger.info("Deleting ${messages.size} messages from queue $queueUrl")

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

fun SqsAsyncClient.acknowledgmentMessageFlow(
    queueUrl: String,
    upstream: Flow<MessageAcknowledgment<out Acknowledgment>>,
    parallelism: Int = 1,
    chunkStrategy: ChunkStrategy = ChunkStrategy.TimeWindow(10, 250.milliseconds)
): Flow<AcknowledgmentResult<SdkResponse>> {
    val deleteMessageChannel: Channel<MessageAcknowledgment<Delete>> = Channel()
    val changeMessageVisibilityChannel: Channel<MessageAcknowledgment<ChangeMessageVisibility>> = Channel()
    val ignoreChannel: Channel<MessageAcknowledgment<Ignore>> = Channel()

    val deleteFlow =
        deleteMessagesFlow(
            queueUrl,
            deleteMessageChannel.receiveAsFlow(),
            parallelism,
            chunkStrategy
        )

    val changeVisibilityFlow =
        changeMessageVisibilityFlow(
            queueUrl,
            changeMessageVisibilityChannel.receiveAsFlow(),
            parallelism,
            chunkStrategy
        )

    val ignoreFlow =
        ignoreChannel
            .receiveAsFlow()
            .map { it to null }

    upstream
        .onCompletion {
            deleteMessageChannel.close()
            ignoreChannel.close()
            changeMessageVisibilityChannel.close()
        }
        .collectAsync { ack: MessageAcknowledgment<out Acknowledgment> ->
            @Suppress("UNCHECKED_CAST")
            val channel = when (ack.acknowledgment) {
                is ChangeMessageVisibility -> changeMessageVisibilityChannel
                Delete -> deleteMessageChannel
                Ignore -> ignoreChannel
            } as Channel<MessageAcknowledgment<out Acknowledgment>>

            channel.send(ack)
        }

    return merge(deleteFlow, changeVisibilityFlow, ignoreFlow)
        .map { (ack, response) -> AcknowledgmentResult(ack.message, ack.acknowledgment, response) }
}

fun Message.acknowledgeWith(acknowledgment: Acknowledgment) =
    MessageAcknowledgment(this, acknowledgment)
