package com.river.connector.aws.sqs

import aws.sdk.kotlin.services.sqs.SqsClient
import aws.sdk.kotlin.services.sqs.model.*
import aws.sdk.kotlin.services.sqs.sendMessageBatch
import com.river.connector.aws.sqs.internal.validate
import com.river.connector.aws.sqs.model.*
import com.river.connector.aws.sqs.model.Acknowledgment.*
import com.river.connector.aws.sqs.model.SqsResult.Success
import com.river.core.*
import com.river.core.ConcurrencyStrategy.Companion.increaseByOne
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlin.time.Duration.Companion.milliseconds

/**
 * Continuously receives messages from an Amazon Simple Queue Service (SQS) queue using the provided [SqsClient].
 * The received messages are returned as a [Flow] of [Message] objects.
 *
 * @param concurrency The [ConcurrencyStrategy] to control the number of concurrent polling operations allowed. Defaults to a static strategy with concurrency of 1.
 * @param stopOnEmptyList If true, the flow will stop when an empty list of messages is received. Defaults to false.
 * @param builder A lambda with receiver for configuring the [ReceiveMessageRequest.Builder] for the underlying receive operation.
 *
 * @return A [Flow] of [Message] objects representing the messages received from the SQS queue.
 *
 * Example usage:
 *
 * ```
 *  val sqsClient = SqsClient {  }
 *
 *  sqsClient
 *      .receiveMessagesAsFlow {
 *          // This builder function is evaluated only once.
 *          queueUrl = sqsClient.getQueueUrl { queueName = "myqueue" }.queueUrl
 *          maxNumberOfMessages = 10
 *          waitTimeSeconds = 20
 *      }
 *      .collect { message -> println("Received message: ${message.body}") }
 * ```
 */
inline fun SqsClient.receiveMessagesAsFlow(
    concurrency: ConcurrencyStrategy = ConcurrencyStrategy.disabled,
    stopOnEmptyList: Boolean = false,
    crossinline builder: suspend ReceiveMessageRequest.Builder.() -> Unit
): Flow<Message> =
    flow {
        val request = ReceiveMessageRequest {}.copy { builder() }

        val elements =
            poll(concurrency, stopOnEmptyList) {
                receiveMessage(request)
                    .messages
                    .orEmpty()
            }

        emitAll(elements)
    }

/**
 * Sends messages to an Amazon Simple Queue Service (SQS) queue efficiently using the provided [SqsClient].
 * Messages are collected from an upstream [Flow] of [SendMessageRequest] objects and sent in batches for optimal performance.
 *
 * @param queue The Amazon SQS queue where messages will be sent.
 * @param upstream A [Flow] of [SendMessageRequest] objects representing messages to be sent to the SQS queue.
 * @param concurrency The maximum number of concurrent send operations. Defaults to 1 for sequential sending.
 * @param groupStrategy Determines how messages are grouped into batches for sending. Defaults to [GroupStrategy.TimeWindow], which groups messages within a specified time window with a maximum batch size.
 *
 * @return A [Flow] of [SendMessageResult], representing the outcome of each message sending attempt:
 *    * [Success]: Indicates successful message delivery.
 *    * [Failure]: Indicates a failed send attempt.
 *
 * **Example usage:**
 *
 * ```kotlin
 * val sqsClient = SqsClient {  }
 * val messages = flowOf(
 *     SendMessageRequest { messageBody = "Message 1" },
 *     SendMessageRequest { messageBody = "Message 2" },
 *     SendMessageRequest { messageBody = "Message 3" }
 * )
 *
 * sqsClient
 *     .sendMessageFlow(
 *         queue = SqsQueue.name("hello-queue"),
 *         upstream = messages,
 *         concurrency = 10,
 *         // SQS limits the max number of messages sent within a single request, it cannot be higher than 10
 *         groupStrategy: GroupStrategy = GroupStrategy.TimeWindow(10, 250.milliseconds)
 *     )
 *     .collect { result ->
 *         when (result) {
 *             is Success -> println("Message sent successfully with ID: ${result.success.messageId}")
 *             is Failure -> println("Failed to send message: ${result.error.messageId}")
 *         }
 *     }
 * ```
 */
fun SqsClient.sendMessageFlow(
    queue: SqsQueue,
    upstream: Flow<SendMessageRequest>,
    concurrency: Int = 1,
    groupStrategy: GroupStrategy = GroupStrategy.TimeWindow(10, 250.milliseconds),
): Flow<SendMessageResult> = groupStrategy.validate().let {
    flow {
        val url = queue.url()

        upstream
            .chunked(groupStrategy)
            .mapAsync(concurrency) { chunk ->
                chunk to sendMessageBatch {
                    queueUrl = url
                    entries = chunk
                        .mapIndexed { index, request ->
                            SendMessageBatchRequestEntry {
                                id = "$index"
                                messageBody = request.messageBody
                                messageAttributes = request.messageAttributes
                                messageGroupId = request.messageGroupId
                                messageSystemAttributes = request.messageSystemAttributes
                                messageDeduplicationId = request.messageDeduplicationId
                            }
                        }
                }
            }
            .flatMapIterable { (chunk, response) ->
                val successes =
                    response
                        .successful
                        .map { Success(it.id.toInt(), chunk[it.id.toInt()], it) }

                val failures =
                    response
                        .failed
                        .map { SqsResult.Failure(it.id.toInt(), chunk[it.id.toInt()], it) }

                (successes + failures).sortedBy { it.batchIndex }
            }
            .let { emitAll(it) }
    }
}

/**
 * Creates a flow that efficiently changes the visibility of messages in an Amazon Simple Queue Service (SQS) queue.
 *
 * This function takes an [upstream] flow of [MessageAcknowledgment] objects and processes them
 * concurrently using [concurrency] and the specified [groupStrategy].
 *
 * @param queue The [SqsQueue] representing the target SQS queue.
 * @param upstream A [Flow] of [MessageAcknowledgment] objects.
 * @param concurrency The level of concurrency for processing messages.
 * @param groupStrategy The strategy to use when chunking messages for processing.
 *
 * @return A [Flow] of [ChangeMessageVisibilityResult], representing the outcome of each operation attempt:
 *    * [Success]: Indicates successful change message visibility operation.
 *    * [Failure]: Indicates a failed change message visibility operation.
 *
 * Example usage:
 *
 * ```
 * val sqsClient = SqsClient {  }
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
fun SqsClient.changeMessageVisibilityFlow(
    queue: SqsQueue,
    upstream: Flow<MessageAcknowledgment<ChangeMessageVisibility>>,
    concurrency: Int = 1,
    groupStrategy: GroupStrategy = GroupStrategy.TimeWindow(10, 250.milliseconds),
): Flow<ChangeMessageVisibilityResult> = groupStrategy.validate().let {
    flow {
        val url = queue.url()

        upstream
            .chunked(groupStrategy)
            .map { chunk ->
                chunk to ChangeMessageVisibilityBatchRequest {
                    queueUrl = url

                    entries = chunk
                        .mapIndexed { index, result ->
                            ChangeMessageVisibilityBatchRequestEntry {
                                visibilityTimeout = result.acknowledgment.timeoutInSeconds
                                receiptHandle = result.message.receiptHandle
                                id = "$index"
                            }
                        }
                }
            }
            .mapAsync(concurrency) { (chunk, request) -> chunk to changeMessageVisibilityBatch(request) }
            .flatMapIterable { (chunk, response) ->
                val successes =
                    response
                        .successful
                        .map { Success(it.id.toInt(), chunk[it.id.toInt()], it) }

                val failures =
                    response
                        .failed
                        .map { SqsResult.Failure(it.id.toInt(), chunk[it.id.toInt()], it) }

                (successes + failures).sortedBy { it.batchIndex }
            }
            .also { emitAll(it) }
    }
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
 * val sqsClient: SqsClient = ...
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
fun SqsClient.deleteMessagesFlow(
    queue: SqsQueue,
    upstream: Flow<MessageAcknowledgment<Delete>>,
    concurrency: Int = 1,
    groupStrategy: GroupStrategy = GroupStrategy.TimeWindow(10, 250.milliseconds),
): Flow<MessageDeleteResult> = groupStrategy.validate().let {
    flow {
        val url = queue.url()

        upstream
            .chunked(groupStrategy)
            .map { chunk ->
                chunk to DeleteMessageBatchRequest {
                    queueUrl = url

                    entries = chunk
                        .mapIndexed { index, result ->
                            DeleteMessageBatchRequestEntry {
                                receiptHandle = result.message.receiptHandle
                                id = "$index"
                            }
                        }
                }
            }
            .mapAsync(concurrency) { (chunk, request) -> chunk to deleteMessageBatch(request) }
            .flatMapIterable { (chunk, response) ->
                val successes =
                    response
                        .successful
                        .map { Success(it.id.toInt(), chunk[it.id.toInt()], it) }

                val failures =
                    response
                        .failed
                        .map { SqsResult.Failure(it.id.toInt(), chunk[it.id.toInt()], it) }

                (successes + failures).sortedBy { it.batchIndex }
            }
            .also { emitAll(it) }
    }
}

/**
 * Creates a flow that processes acknowledgments for messages in an Amazon Simple Queue Service (SQS) queue.
 *
 * This function takes an [upstream] flow of [MessageAcknowledgment] objects and processes them
 * based on their acknowledgment type: [ChangeMessageVisibility], [Delete], or [Ignore].
 * The function processes acknowledgments concurrently using [concurrency] and the specified [groupStrategy].
 *
 * @param queue The reference of the queue.
 * @param upstream A [Flow] of [MessageAcknowledgment] objects.
 * @param concurrency The level of concurrency for processing messages.
 * @param groupStrategy The strategy to use when chunking messages for processing.
 *
 * @return A [Flow] of [MessageAcknowledgmentResult] objects that contain the message, acknowledgment, and response.
 *
 * Example usage:
 * ```
 * val sqsClient: SqsClient = ...
 * val messageAcknowledgments = flowOf(
 *     MessageAcknowledgment(Message(...), Delete),
 *     MessageAcknowledgment(Message(...), ChangeMessageVisibility(30)),
 *     MessageAcknowledgment(Message(...), Ignore)
 * )
 *
 * sqsClient
 *     .acknowledgmentMessageFlow(SqsQueue.name("myqueue"), messageAcknowledgments)
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
fun SqsClient.acknowledgmentMessageFlow(
    queue: SqsQueue,
    upstream: Flow<MessageAcknowledgment<Acknowledgment>>,
    concurrency: Int = 1,
    groupStrategy: GroupStrategy = GroupStrategy.TimeWindow(10, 250.milliseconds),
): Flow<MessageAcknowledgmentResult> = groupStrategy.validate().let {
    flow {
        val deleteMessageChannel: Channel<MessageAcknowledgment<Delete>> = Channel()
        val changeMessageVisibilityChannel: Channel<MessageAcknowledgment<ChangeMessageVisibility>> = Channel()
        val ignoreChannel: Channel<MessageAcknowledgment<Ignore>> = Channel()

        val deleteFlow =
            deleteMessagesFlow(
                queue,
                deleteMessageChannel.receiveAsFlow(),
                concurrency,
                groupStrategy
            )

        val changeVisibilityFlow =
            changeMessageVisibilityFlow(
                queue,
                changeMessageVisibilityChannel.receiveAsFlow(),
                concurrency,
                groupStrategy
            )

        val ignoreFlow =
            ignoreChannel
                .receiveAsFlow()
                .map { Success(-1, it, Unit) }

        val coroutine = CoroutineScope(Dispatchers.Default)

        with(coroutine) {
            upstream
                .onCompletion {
                    deleteMessageChannel.close()
                    ignoreChannel.close()
                    changeMessageVisibilityChannel.close()
                }
                .launchCollect { ack: MessageAcknowledgment<Acknowledgment> ->
                    @Suppress("UNCHECKED_CAST")
                    val channel = when (ack.acknowledgment) {
                        is ChangeMessageVisibility -> changeMessageVisibilityChannel
                        Delete -> deleteMessageChannel
                        Ignore -> ignoreChannel
                    } as Channel<MessageAcknowledgment<Acknowledgment>>

                    channel.send(ack)
                }
        }

        emitAll(merge(deleteFlow, changeVisibilityFlow, ignoreFlow))
        coroutine.cancel()
    }
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
 *      .map { message -> message acknowledgeWith Acknowledgment.Delete }
 *      .let { flow -> sqsClient.acknowledgmentMessageFlow(queue, flow) }
 *      .collect(::println)
 * ```
 */
infix fun Message.acknowledgeWith(acknowledgment: Acknowledgment) =
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
 * @param queue The reference of the queue from which messages will be received.
 * @param concurrency The level of concurrency for processing messages. Defaults to 1.
 * @param groupStrategy The strategy to use when chunking messages for processing. Defaults to [GroupStrategy.TimeWindow].
 * @param receiveConfiguration A lambda with receiver for configuring the [ReceiveConfiguration] for the underlying receive operation.
 * @param commitConfiguration A lambda with receiver for configuring the [CommitConfiguration] for the underlying commit operation.
 * @param onError The strategy to be used when the message processing encounters an error. Defaults to [OnError.Retry].
 * @param onMessages A function to process received messages. It receives a list of messages and returns a list of [MessageAcknowledgment].
 *
 * @return A [Job] representing the execution of the flow.
 *
 * Example usage:
 *
 * ```
 * val sqsClient: SqsClient = ...
 *
 * coroutineScope {
 *     val myQueueJob = sqsClient.onMessages(SqsQueue.name("myqueue")) { messages ->
 *         messages.map { MessageAcknowledgment(it, Delete) }
 *     }
 *
 *     //You may cancel myQueueJob at any time.
 * }
 * ```
 */
context(CoroutineScope)
fun SqsClient.onMessages(
    queue: SqsQueue,
    concurrency: Int = 1,
    groupStrategy: GroupStrategy = GroupStrategy.TimeWindow(10, 250.milliseconds),
    receiveConfiguration: ReceiveConfiguration.() -> Unit = {},
    commitConfiguration: CommitConfiguration.() -> Unit = {},
    onError: OnError = OnError.Retry(250.milliseconds),
    onMessages: suspend (List<Message>) -> List<MessageAcknowledgment<Acknowledgment>>
): Job = groupStrategy.validate().let {
    launch {
        val receiveConfig = ReceiveConfiguration().also(receiveConfiguration)
        val commitConfig = CommitConfiguration().also(commitConfiguration)

        val url = queue.url()

        val messagesFlow =
            receiveMessagesAsFlow(increaseByOne(receiveConfig.concurrency), receiveConfig.stopOnEmptyList) {
                receiveConfig.request(this)
                queueUrl = url
            }

        val processingFlow =
            messagesFlow
                .chunked(groupStrategy)
                .mapAsync(concurrency) { onMessages(it) }
                .flattenIterable()

        val acknowledgmentFlow =
            acknowledgmentMessageFlow(
                queue = SqsQueue.url(url),
                upstream = processingFlow,
                concurrency = commitConfig.concurrency,
                groupStrategy = commitConfig.groupStrategy
            )

        acknowledgmentFlow
            .retryWhen { cause, attempt ->
                when (onError) {
                    OnError.Complete -> false
                    OnError.Throw -> throw cause
                    is OnError.Retry -> onError.maxAttempts < attempt
                }
            }
            .collect()
    }
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
 * @param queue The reference of the queue from which messages will be received.
 * @param concurrency The level of concurrency for processing messages. Defaults to 1.
 * @param receiveConfiguration A lambda with receiver for configuring the [ReceiveConfiguration] for the underlying receive operation.
 * @param commitConfiguration A lambda with receiver for configuring the [CommitConfiguration] for the underlying commit operation.
 * @param onError The strategy to be used when the message processing encounters an error. Defaults to [OnError.Retry].
 * @param onMessage A function to process a received message. It receives a single message and returns a [MessageAcknowledgment].
 *
 * @return A [Job] representing the execution of the flow.
 *
 * Example usage:
 *
 * ```
 * val sqsClient: SqsClient = ...
 *
 * coroutineScope {
 *     val myQueueJob = sqsClient.onMessage(SqsQueue.name("myqueue")) { message ->
 *         Delete
 *     }
 *
 *     // You may cancel myQueueJob at any time.
 * }
 * ```
 */
context(CoroutineScope)
fun SqsClient.onMessage(
    queue: SqsQueue,
    concurrency: Int = 1,
    receiveConfiguration: ReceiveConfiguration.() -> Unit = {},
    commitConfiguration: CommitConfiguration.() -> Unit = {},
    onError: OnError = OnError.Retry(250.milliseconds),
    onMessage: suspend (Message) -> Acknowledgment
): Job = onMessages(
    queue = queue,
    concurrency = concurrency,
    groupStrategy = GroupStrategy.Count(1),
    receiveConfiguration = receiveConfiguration,
    commitConfiguration = commitConfiguration,
    onError = onError
) { messages -> messages.map { MessageAcknowledgment(it, onMessage(it)) } }
