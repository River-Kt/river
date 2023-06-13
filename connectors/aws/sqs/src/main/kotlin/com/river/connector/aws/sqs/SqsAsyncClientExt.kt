package com.river.connector.aws.sqs

import com.river.connector.aws.sqs.model.*
import com.river.connector.aws.sqs.model.Acknowledgment.*
import com.river.connector.aws.sqs.model.SendMessageRequest
import com.river.connector.aws.sqs.model.SendMessageResponse
import com.river.core.*
import com.river.core.ConcurrencyStrategy.Companion.increaseByOne
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.future.await
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import software.amazon.awssdk.core.SdkResponse
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.*
import kotlin.time.Duration.Companion.milliseconds

internal val SqsAsyncClient.logger: Logger
    get() = LoggerFactory.getLogger(javaClass)

/**
 * Continuously receives messages from an Amazon Simple Queue Service (SQS) queue using the provided [SqsAsyncClient].
 * The received messages are returned as a [Flow] of [Message] objects.
 *
 * @param concurrency The [ConcurrencyStrategy] to control the number of concurrent polling operations allowed. Defaults to a static strategy with concurrency of 1.
 * @param stopOnEmptyList If true, the flow will stop when an empty list of messages is received. Defaults to false.
 * @param builder A lambda with receiver for configuring the [ReceiveMessageRequestBuilder] for the underlying receive operation.
 *
 * @return A [Flow] of [Message] objects representing the messages received from the SQS queue.
 *
 * Example usage:
 *
 * ```
 *  val sqsClient = SqsAsyncClient.create()
 *
 *  sqsClient.receiveMessagesAsFlow {
 *      queueUrl(sqsClient.getQueueUrlByName("myqueue"))
 *      maxNumberOfMessages(10)
 *      waitTimeSeconds(20)
 *  }.collect { message -> println("Received message: ${message.body()}") }
 * ```
 */
fun SqsAsyncClient.receiveMessagesAsFlow(
    concurrency: ConcurrencyStrategy = ConcurrencyStrategy.disabled,
    stopOnEmptyList: Boolean = false,
    builder: suspend ReceiveMessageRequestBuilder.() -> Unit
): Flow<Message> =
    flow {
        val request = ReceiveMessageRequestBuilder().also { builder(it) }.build()

        val elements =
            poll(concurrency, stopOnEmptyList) {
                receiveMessage(request)
                    .await()
                    .messages()
            }

        emitAll(elements)
    }

/**
 * Sends messages to an Amazon Simple Queue Service (SQS) queue using the provided [SqsAsyncClient].
 * Messages are consumed from an upstream [Flow] of [SendMessageRequest] objects.
 *
 * @param upstream A [Flow] of [SendMessageRequest] objects to be sent to the specified SQS queue.
 * @param concurrency The number of concurrent send operations. Defaults to 1.
 * @param groupStrategy Determines how to group messages for sending in batches. Defaults to [GroupStrategy.TimeWindow].
 * @param queueUrl A lambda function returning the URL of the Amazon SQS queue to which messages will be sent.

 * @return A [Flow] of [SendMessageResponse], which can be either [SendMessageResponse.Successful] or
 *         [SendMessageResponse.Failure].
 *
 * Example usage:
 *
 * ```
 *  val sqsClient = SqsAsyncClient.create()
 *  val messages = flowOf(RequestMessage("Message 1"), RequestMessage("Message 2"), RequestMessage("Message 3"))
 *
 *  sqsClient
 *      .sendMessageFlow(messages) {
 *          sqsClient.getQueueUrlByName("myqueue")
 *      }
 *      .collect { response -> println("Batch sent with messageId: ${response.batchItemId()}") }
 * ```
 */
fun SqsAsyncClient.sendMessageFlow(
    upstream: Flow<SendMessageRequest>,
    concurrency: Int = 1,
    groupStrategy: GroupStrategy = GroupStrategy.TimeWindow(10, 250.milliseconds),
    queueUrl: suspend () -> String,
): Flow<SendMessageResponse> =
    flowOfSuspend(queueUrl).flatMapConcat { url ->
        upstream
            .map { it.asMessageRequestEntry() }
            .chunked(groupStrategy)
            .mapAsync(concurrency) { entries ->
                val response =
                    sendMessageBatch { it.queueUrl(url).entries(entries) }
                        .await()

                val successful = response.successful().map {
                    SendMessageResponse.Successful(
                        id = it.id(),
                        messageId = it.messageId(),
                        sequenceNumber = it.sequenceNumber(),
                        md5OfMessageBody = it.md5OfMessageBody(),
                        md5OfMessageAttributes = it.md5OfMessageAttributes(),
                        md5OfMessageSystemAttributes = it.md5OfMessageSystemAttributes(),
                        internalBatchResponse = response
                    )
                }

                val failed = response.failed().map {
                    SendMessageResponse.Failure(
                        id = it.id(),
                        code = it.code(),
                        message = it.message(),
                        senderFault = it.senderFault(),
                        internalBatchResponse = response
                    )
                }

                (successful + failed).sortedBy { it.id }
            }
            .flattenIterable()
    }

/**
 * Creates a flow that changes the visibility of messages in an Amazon Simple Queue Service (SQS) queue.
 *
 * This function takes an [upstream] flow of [MessageAcknowledgment] objects and processes them
 * concurrently using [concurrency] and the specified [groupStrategy].
 *
 * @param upstream A [Flow] of [MessageAcknowledgment] objects.
 * @param concurrency The level of concurrency for processing messages.
 * @param groupStrategy The strategy to use when chunking messages for processing.
 * @param queueUrl A lambda function returning the URL of the Amazon SQS queue.
 *
 * @return A [Flow] that emits pairs of [MessageAcknowledgment] and [ChangeMessageVisibilityBatchResponse].
 *
 * Example usage:
 *
 * ```
 * val sqsClient = SqsAsyncClient.create()
 *
 * val acknowledgmentsFlow: Flow<MessageAcknowledgment<ChangeMessageVisibility>> = // ... create a flow
 *
 * val resultFlow =
 *     sqsClient
 *         .changeMessageVisibilityFlow(acknowledgmentsFlow) {
 *             sqsClient.getQueueUrlByName("myqueue")
 *         }
 *
 * resultFlow.collect { (acknowledgment, response) ->
 *     // Process the acknowledgment and response
 * }
 * ```
 */
fun SqsAsyncClient.changeMessageVisibilityFlow(
    upstream: Flow<MessageAcknowledgment<ChangeMessageVisibility>>,
    concurrency: Int = 1,
    groupStrategy: GroupStrategy = GroupStrategy.TimeWindow(10, 250.milliseconds),
    queueUrl: suspend () -> String
): Flow<Pair<MessageAcknowledgment<ChangeMessageVisibility>, ChangeMessageVisibilityBatchResponse>> =
    flowOfSuspend(queueUrl).flatMapConcat { url ->
        upstream
            .chunked(groupStrategy)
            .mapAsync(concurrency) { messages ->
                changeMessageVisibilityBatch {
                    it.queueUrl(url)

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
            .flattenIterable()
    }

/**
 * Creates a flow that deletes messages from an Amazon Simple Queue Service (SQS) queue.
 *
 * This function takes an [upstream] flow of [MessageAcknowledgment] objects with a [Delete]
 * acknowledgment and processes them concurrently using [concurrency] and the specified [groupStrategy].
 *
 * @param upstream A [Flow] of [MessageAcknowledgment] objects with a [Delete] acknowledgment.
 * @param concurrency The level of concurrency for processing messages.
 * @param groupStrategy The strategy to use when chunking messages for processing.
 * @param queueUrl A lambda function returning the URL of the Amazon SQS queue.
 *
 * @return A [Flow] that emits pairs of [MessageAcknowledgment] and [DeleteMessageBatchResponse].
 *
 * Example usage:
 * ```
 * val sqsClient: SqsAsyncClient = ...
 * val messageAcknowledgments = flowOf(
 *     MessageAcknowledgment(Message(...), Delete),
 *     MessageAcknowledgment(Message(...), Delete)
 * )
 *
 * sqsClient
 *     .deleteMessagesFlow(messageAcknowledgments) {
 *         sqsClient.getQueueUrlByName("myqueue")
 *     }
 *     .collect { (ack, response) ->
 *         println("Deleted message: ${ack.message.messageId()}, response: $response")
 *     }
 * ```
 */
fun SqsAsyncClient.deleteMessagesFlow(
    upstream: Flow<MessageAcknowledgment<Delete>>,
    concurrency: Int = 1,
    groupStrategy: GroupStrategy = GroupStrategy.TimeWindow(10, 250.milliseconds),
    queueUrl: suspend () -> String
): Flow<Pair<MessageAcknowledgment<Delete>, DeleteMessageBatchResponse>> =
    flowOfSuspend(queueUrl).flatMapConcat { url ->
        upstream
            .chunked(groupStrategy)
            .mapAsync(concurrency) { messages ->
                deleteMessageBatch {
                    logger.info("Deleting ${messages.size} messages from queue $queueUrl")

                    it.queueUrl(url)

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
            .flattenIterable()
    }

/**
 * Creates a flow that processes acknowledgments for messages in an Amazon Simple Queue Service (SQS) queue.
 *
 * This function takes an [upstream] flow of [MessageAcknowledgment] objects and processes them
 * based on their acknowledgment type: [ChangeMessageVisibility], [Delete], or [Ignore].
 * The function processes acknowledgments concurrently using [concurrency] and the specified [groupStrategy].
 *
 * @param upstream A [Flow] of [MessageAcknowledgment] objects.
 * @param concurrency The level of concurrency for processing messages.
 * @param groupStrategy The strategy to use when chunking messages for processing.
 * @param queueUrl A lambda function returning the URL of the Amazon SQS queue.
 *
 * @return A [Flow] of [AcknowledgmentResult] objects that contain the message, acknowledgment, and response.
 *
 * Example usage:
 * ```
 * val sqsClient: SqsAsyncClient = ...
 * val messageAcknowledgments = flowOf(
 *     MessageAcknowledgment(Message(...), Delete),
 *     MessageAcknowledgment(Message(...), ChangeMessageVisibility(30)),
 *     MessageAcknowledgment(Message(...), Ignore)
 * )
 *
 * sqsClient
 *     .acknowledgmentMessageFlow(messageAcknowledgments) {
 *         sqsClient.getQueueUrlByName("myqueue")
 *     }
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
    upstream: Flow<MessageAcknowledgment<out Acknowledgment>>,
    concurrency: Int = 1,
    groupStrategy: GroupStrategy = GroupStrategy.TimeWindow(10, 250.milliseconds),
    queueUrl: suspend () -> String
): Flow<AcknowledgmentResult<SdkResponse>> = flowOfSuspend(queueUrl).flatMapConcat { url ->
    val deleteMessageChannel: Channel<MessageAcknowledgment<Delete>> = Channel()
    val changeMessageVisibilityChannel: Channel<MessageAcknowledgment<ChangeMessageVisibility>> = Channel()
    val ignoreChannel: Channel<MessageAcknowledgment<Ignore>> = Channel()

    val deleteFlow =
        deleteMessagesFlow(
            deleteMessageChannel.receiveAsFlow(),
            concurrency,
            groupStrategy
        ) { url }

    val changeVisibilityFlow =
        changeMessageVisibilityFlow(
            changeMessageVisibilityChannel.receiveAsFlow(),
            concurrency,
            groupStrategy
        ) { url }

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
        .launchCollect { ack: MessageAcknowledgment<out Acknowledgment> ->
            @Suppress("UNCHECKED_CAST")
            val channel = when (ack.acknowledgment) {
                is ChangeMessageVisibility -> changeMessageVisibilityChannel
                Delete -> deleteMessageChannel
                Ignore -> ignoreChannel
            } as Channel<MessageAcknowledgment<out Acknowledgment>>

            channel.send(ack)
        }

    merge(deleteFlow, changeVisibilityFlow, ignoreFlow)
        .map { (ack, response) -> AcknowledgmentResult(ack.message, ack.acknowledgment, response) }
}

/**
 * Retrieves the queue URL for the specified queue name by making an asynchronous request to the Amazon SQS service.
 *
 * This function returns a [String] representing the queue URL. If the queue with the given
 * name is not found, an exception will be thrown.
 *
 * Example usage:
 *
 * ```
 * val sqsClient: SqsAsyncClient = ...
 *
 * val url = sqsClient.getQueueUrlByName("myqueue")
 * println("Queue URL: $url")
 * ```
 *
 * @param name The name of the queue for which the URL needs to be fetched.
 * @return A URL of the queue
 */
suspend fun SqsAsyncClient.getQueueUrlByName(name: String): String =
    getQueueUrl { it.queueName(name) }
        .await()
        .queueUrl()

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
 *      .let { flow -> sqsClient.acknowledgmentMessageFlow(flow) { queueUrl } }
 *      .collect(::println)
 * ```
 */
fun Message.acknowledgeWith(acknowledgment: Acknowledgment) =
    MessageAcknowledgment(this, acknowledgment)

/**
 * This function is a high-level abstraction that combines receiving messages, processing them, and sending acknowledgments.
 * It is built on top of the [receiveMessagesAsFlow] and [acknowledgmentMessageFlow] functions.
 *
 * Creates a flow that continuously receives messages from an Amazon Simple Queue Service (SQS) queue, processes them
 * using a provided function, and sends acknowledgments for processed messages.
 *
 * The function is executed in the specified [CoroutineScope] and returns a [Job] that represents its execution.
 *
 * @param queueName The name of the queue from which messages will be received.
 * @param concurrency The level of concurrency for processing messages. Defaults to 1.
 * @param groupStrategy The strategy to use when chunking messages for processing. Defaults to [GroupStrategy.TimeWindow].
 * @param receiveConfiguration A lambda with receiver for configuring the [ReceiveConfiguration] for the underlying receive operation.
 * @param commitConfiguration A lambda with receiver for configuring the [CommitConfiguration] for the underlying commit operation.
 * @param f A function to process received messages. It receives a list of messages and returns a list of [MessageAcknowledgment].
 *
 * @return A [Job] representing the execution of the flow.
 *
 * Example usage:
 *
 * ```
 * val sqsClient: SqsAsyncClient = ...
 *
 * coroutineScope {
 *     val myQueueJob = sqsClient.onMessages("myqueue") { messages ->
 *         messages.map { MessageAcknowledgment(it, Delete) }
 *     }
 *
 *     //You may cancel myQueueJob at any time.
 * }
 * ```
 */
context(CoroutineScope)
suspend fun SqsAsyncClient.onMessages(
    queueName: String,
    concurrency: Int = 1,
    groupStrategy: GroupStrategy = GroupStrategy.TimeWindow(10, 250.milliseconds),
    receiveConfiguration: ReceiveConfiguration.() -> Unit = {},
    commitConfiguration: CommitConfiguration.() -> Unit = {},
    f: suspend (List<Message>) -> List<MessageAcknowledgment<Acknowledgment>>
): Job {
    val url = getQueueUrlByName(queueName)

    val receiveConfig = ReceiveConfiguration().also(receiveConfiguration)
    val commitConfig = CommitConfiguration().also(commitConfiguration)

    val messagesFlow =
        receiveMessagesAsFlow(increaseByOne(receiveConfig.concurrency), receiveConfig.stopOnEmptyList) {
            receiveConfig.request(this)
            queueUrl = url
        }

    val processingFlow =
        messagesFlow
            .chunked(groupStrategy)
            .mapAsync(concurrency) { f(it) }
            .flattenIterable()

    val acknowledgmentFlow =
        acknowledgmentMessageFlow(
            upstream = processingFlow,
            concurrency = commitConfig.concurrency,
            groupStrategy = commitConfig.groupStrategy
        ) { url }

    return acknowledgmentFlow.launchCollect()
}

/**
 * This function is a high-level abstraction that combines receiving messages, processing them, and sending acknowledgments.
 * It is built on top of the [receiveMessagesAsFlow] and [acknowledgmentMessageFlow] functions.
 *
 * Creates a flow that continuously receives messages from an Amazon Simple Queue Service (SQS) queue, processes them
 * using a provided function, and sends acknowledgments for processed messages.
 *
 * This function is a simplified version of [onMessages] that processes messages one by one.
 * It is executed in the specified [CoroutineScope] and returns a [Job] that represents its execution.
 *
 * @param queueName The name of the queue from which messages will be received.
 * @param concurrency The level of concurrency for processing messages. Defaults to 1.
 * @param receiveConfiguration A lambda with receiver for configuring the [ReceiveConfiguration] for the underlying receive operation.
 * @param commitConfiguration A lambda with receiver for configuring the [CommitConfiguration] for the underlying commit operation.
 * @param f A function to process a received message. It receives a single message and returns a [MessageAcknowledgment].
 *
 * @return A [Job] representing the execution of the flow.
 *
 * Example usage:
 *
 * ```
 * val sqsClient: SqsAsyncClient = ...
 *
 * coroutineScope {
 *     val myQueueJob = sqsClient.onMessage("myqueue") { message ->
 *         MessageAcknowledgment(message, Delete)
 *     }
 *
 *     // You may cancel myQueueJob at any time.
 * }
 * ```
 */
context(CoroutineScope)
suspend fun SqsAsyncClient.onMessage(
    queueName: String,
    concurrency: Int = 1,
    receiveConfiguration: ReceiveConfiguration.() -> Unit = {},
    commitConfiguration: CommitConfiguration.() -> Unit = {},
    f: suspend (Message) -> MessageAcknowledgment<Acknowledgment>
): Job = onMessages(
    queueName = queueName,
    concurrency = concurrency,
    groupStrategy = GroupStrategy.Count(1),
    receiveConfiguration = receiveConfiguration,
    commitConfiguration = commitConfiguration
) { listOf(f(it.first())) }
