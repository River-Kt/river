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

/**
 * Continuously receives messages from an Amazon Simple Queue Service (SQS) queue using the provided [SqsAsyncClient].
 * The received messages are returned as a [Flow] of [Message] objects.
 *
 * @param maxParallelism The maximum number of parallel receive operations. Defaults to 1.
 * @param stopOnEmptyList If true, the flow will stop when an empty list of messages is received. Defaults to false.
 * @param minimumParallelism The minimum number of parallel receive operations. Defaults to 1.
 * @param increaseStrategy Determines how the parallelism increases when processing messages. Defaults to [ParallelismIncreaseStrategy.ByOne].
 * @param builder A lambda with receiver for configuring the [ReceiveMessageRequestBuilder] for the underlying receive operation.
 *
 * @return A [Flow] of [Message] objects representing the messages received from the SQS queue.
 *
 * Example usage:
 *
 * ```
 *  val sqsClient = SqsAsyncClient.create()
 *  val queueUrl = "https://sqs.us-east-1.amazonaws.com/123456789012/myqueue"
 *
 *  sqsClient.receiveMessagesFlow {
 *      queueUrl(queueUrl)
 *      maxNumberOfMessages(10)
 *      waitTimeSeconds(20)
 *  }.collect { message -> println("Received message: ${message.body()}") }
 * ```
 */
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

/**
 * Sends messages to an Amazon Simple Queue Service (SQS) queue using the provided [SqsAsyncClient].
 * Messages are consumed from an upstream [Flow] of [RequestMessage] objects.
 *
 * @param queueUrl The URL of the Amazon SQS queue to which messages will be sent.
 * @param upstream A [Flow] of [RequestMessage] objects to be sent to the specified SQS queue.
 * @param parallelism The number of concurrent send operations. Defaults to 1.
 * @param chunkStrategy Determines how to group messages for sending in batches. Defaults to [ChunkStrategy.TimeWindow].
 *
 * @return A [Flow] of [SendMessageBatchResponse] objects representing the results of sending messages in batches.
 *
 * Example usage:
 *
 * ```
 *  val sqsClient = SqsAsyncClient.create()
 *  val queueUrl = "https://sqs.us-east-1.amazonaws.com/123456789012/myqueue"
 *  val messages = flowOf(RequestMessage("Message 1"), RequestMessage("Message 2"), RequestMessage("Message 3"))
 *
 *  sqsClient
 *      .sendMessageFlow(queueUrl, messages)
 *      .collect { response -> println("Batch sent with messageId: ${response.batchItemId()}") }
 * ```
 */
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

/**
 * Creates a flow that changes the visibility of messages in an Amazon Simple Queue Service (SQS) queue.
 *
 * This function takes an [upstream] flow of [MessageAcknowledgment] objects and processes them
 * in parallel using [parallelism] and the specified [chunkStrategy].
 *
 * @param queueUrl The URL of the SQS queue.
 * @param upstream A [Flow] of [MessageAcknowledgment] objects.
 * @param parallelism The level of parallelism for processing messages.
 * @param chunkStrategy The strategy to use when chunking messages for processing.
 * @return A [Flow] that emits pairs of [MessageAcknowledgment] and [ChangeMessageVisibilityBatchResponse].
 *
 * Example usage:
 *
 * ```
 * val sqsClient = SqsAsyncClient.create()
 * val queueUrl = "https://sqs.region.amazonaws.com/123456789012/queue-name"
 *
 * val acknowledgmentsFlow: Flow<MessageAcknowledgment<ChangeMessageVisibility>> = // ... create a flow
 *
 * val resultFlow = sqsClient.changeMessageVisibilityFlow(queueUrl, acknowledgmentsFlow)
 *
 * resultFlow.collect { (acknowledgment, response) ->
 *     // Process the acknowledgment and response
 * }
 * ```
 */
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

/**
 * Creates a flow that deletes messages from an Amazon Simple Queue Service (SQS) queue.
 *
 * This function takes an [upstream] flow of [MessageAcknowledgment] objects with a [Delete]
 * acknowledgment and processes them in parallel using [parallelism] and the specified [chunkStrategy].
 *
 * @param queueUrl The URL of the SQS queue.
 * @param upstream A [Flow] of [MessageAcknowledgment] objects with a [Delete] acknowledgment.
 * @param parallelism The level of parallelism for processing messages.
 * @param chunkStrategy The strategy to use when chunking messages for processing.
 * @return A [Flow] that emits pairs of [MessageAcknowledgment] and [DeleteMessageBatchResponse].
 *
 * Example usage:
 * ```
 * val sqsClient: SqsAsyncClient = ...
 * val queueUrl = "https://sqs.us-east-1.amazonaws.com/123456789012/myqueue"
 * val messageAcknowledgments = flowOf(
 *     MessageAcknowledgment(Message(...), Delete),
 *     MessageAcknowledgment(Message(...), Delete)
 * )
 *
 * sqsClient.deleteMessagesFlow(queueUrl, messageAcknowledgments)
 *     .collect { (ack, response) ->
 *         println("Deleted message: ${ack.message.messageId()}, response: $response")
 *     }
 * ```
 */
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

/**
 * Creates a flow that processes acknowledgments for messages in an Amazon Simple Queue Service (SQS) queue.
 *
 * This function takes an [upstream] flow of [MessageAcknowledgment] objects and processes them
 * based on their acknowledgment type: [ChangeMessageVisibility], [Delete], or [Ignore].
 * The function processes acknowledgments in parallel using [parallelism] and the specified [chunkStrategy].
 *
 * @param queueUrl The URL of the SQS queue.
 * @param upstream A [Flow] of [MessageAcknowledgment] objects.
 * @param parallelism The level of parallelism for processing messages.
 * @param chunkStrategy The strategy to use when chunking messages for processing.
 * @return A [Flow] of [AcknowledgmentResult] objects that contain the message, acknowledgment, and response.
 *
 * Example usage:
 * ```
 * val sqsClient: SqsAsyncClient = ...
 * val queueUrl = "https://sqs.us-east-1.amazonaws.com/123456789012/myqueue"
 * val messageAcknowledgments = flowOf(
 *     MessageAcknowledgment(Message(...), Delete),
 *     MessageAcknowledgment(Message(...), ChangeMessageVisibility(30)),
 *     MessageAcknowledgment(Message(...), Ignore)
 * )
 *
 * sqsClient.acknowledgmentMessageFlow(queueUrl, messageAcknowledgments)
 *     .collect { result ->
 *         println(
 *             """Processed message:
 *             ${result.message.messageId()},
 *             acknowledgment: ${result.acknowledgment},
 *             response: ${result.response}"""
 *         )
 *     }
 * ```
 */
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

/**
 * Util function to create a MessageAcknowledgment from a [Message] and an [Acknowledgment].
 *
 * Be aware that this function alone do not perform any operation. You must send a [Flow] of [MessageAcknowledgment]
 * to [acknowledgmentMessageFlow] in order to delete or change the visibility of a message.
 *
 * @param acknowledgment The acknowledgment of the received SQS Message
 *
 * @return An [MessageAcknowledgment] object, which is basically a tuple of [Message] and [Acknowledgment].
 *
 * Example usage:
 *
 * ```
 *  val messageFlow: Flow<Message> = ...
 *
 *  messageFlow
 *      .map { message -> message.acknowledgeWith(Acknowledgment.Delete) }
 *      .let { flow -> sqsClient.acknowledgmentMessageFlow(queueUrl, flow) }
 *      .collect(::println)
 * ```
 */
fun Message.acknowledgeWith(acknowledgment: Acknowledgment) =
    MessageAcknowledgment(this, acknowledgment)
