package com.river.connector.aws.sns

import aws.sdk.kotlin.services.sns.SnsClient
import aws.sdk.kotlin.services.sns.model.PublishBatchRequest
import aws.sdk.kotlin.services.sns.model.PublishBatchRequestEntry
import aws.sdk.kotlin.services.sns.model.PublishRequest
import aws.sdk.kotlin.services.sns.model.PublishResponse
import com.river.core.GroupStrategy
import com.river.core.chunked
import com.river.core.flatMapIterable
import com.river.core.mapAsync
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlin.time.Duration.Companion.milliseconds

/**
 * The [publishFlow] function is used to publish messages concurrently to an Amazon Simple Notification Service (SNS)
 * topic using a [SnsClient].
 *
 * It takes an input [Flow] of [PublishRequest] and publishes the messages
 * concurrently, respecting the specified [groupStrategy].
 *
 * The [groupStrategy] parameter defines how the messages should be grouped. Using a time window, for instance, the messages
 * are going to be grouped either when the maximum number of items is reached, or when the time duration has elapsed, whichever
 * comes first. This can help balance between processing latency and the granularity of data aggregation, leading
 * to more efficient processing by potentially reducing the number of requests to AWS.
 *
 * @param topicArn the Amazon Resource Name (ARN) of the SNS topic to publish the messages to.
 * @param upstream The input [Flow] of [PublishMessageRequest]s to be published.
 * @param concurrency The degree of concurrency to use for publishing messages (default is 1).
 * @param groupStrategy The [GroupStrategy] to use for grouping the messages (default is TimeWindow with 10 items and 250 milliseconds).
 *
 * @return A [Flow] of [PublishResponse]s for the published messages.
 *
 * Example usage:
 * ```
 * val snsClient: SnsClient = ...
 * val messages: Flow<PublishMessageRequest> = ...
 * val topicArn = "arn:aws:sns:us-east-1:123456789012:MyTopic"
 *
 * val responses: Flow<PublishResponse> = snsClient.publishFlow(topicArn, messages)
 *
 * responses.collect { response -> println(response) }
 * ```
 */
fun SnsClient.publishFlow(
    topicArn: String,
    upstream: Flow<PublishRequest>,
    concurrency: Int = 1,
    groupStrategy: GroupStrategy = GroupStrategy.TimeWindow(10, 250.milliseconds),
): Flow<PublishResponse> =
    flow {
        emitAll(
            upstream
                .chunked(groupStrategy)
                .map { chunk ->
                    PublishBatchRequest {
                        this.topicArn = topicArn

                        publishBatchRequestEntries = chunk.mapIndexed { index, it ->
                            PublishBatchRequestEntry {
                                id = "$index"
                                message = it.message
                                messageAttributes = it.messageAttributes
                                messageDeduplicationId = it.messageDeduplicationId
                                messageGroupId = it.messageGroupId
                                messageStructure = it.messageStructure
                                subject = it.subject
                            }
                        }
                    }
                }
                .mapAsync(concurrency) { publishBatch(it) }
                .flatMapIterable { response ->
                    if (response.failed.orEmpty().isNotEmpty()) {
                        error("Failed to publish the following messages to SNS: ${response.failed}")
                    }

                    response
                        .successful
                        .orEmpty()
                        .map {
                            it.id to PublishResponse {
                                messageId = it.messageId
                                sequenceNumber = it.sequenceNumber
                            }
                        }
                        .sortedBy { it.first }
                        .map { it.second }
                }
        )
    }

/**
 * The [publishFlow] function is used to publish messages concurrently to an Amazon Simple Notification Service (SNS)
 * topic using a [SnsClient].
 *
 * It takes an input [Flow] of [T], maps to [PublishRequest]  and publishes the messages
 * concurrently, respecting the specified [groupStrategy].
 *
 * The [groupStrategy] parameter defines how the messages should be grouped. Using a time window, for instance, the messages
 * are going to be grouped either when the maximum number of items is reached, or when the time duration has elapsed, whichever
 * comes first. This can help balance between processing latency and the granularity of data aggregation, leading
 * to more efficient processing by potentially reducing the number of requests to AWS.
 *
 *
 * @param topicArn the Amazon Resource Name (ARN) of the SNS topic to publish the messages to.
 * @param concurrency The degree of concurrency to use for publishing messages (default is 1).
 * @param groupStrategy The [GroupStrategy] to use for grouping the messages (default is TimeWindow with 10 items and 250 milliseconds).
 * @param builder The builder that maps [T] to [PublishRequest]
 *
 * @return A [Flow] of [PublishResponse]s for the published messages.
 *
 * Example usage:
 * ```
 * val snsClient: SnsClient = ...
 * val numbers: Flow<Int> = (1..20).asFlow()
 * val topicArn = "arn:aws:sns:us-east-1:123456789012:MyTopic"
 *
 * val responses: Flow<PublishMessageResponse> = with(snsClient) {
 *     numbers.publishFlow(topicArn) { number ->
 *         message = "$number"
 *     }
 * }
 *
 * responses.collect { response -> println(response) }
 * ```
 */
context(SnsClient)
fun <T> Flow<T>.publishFlow(
    topicArn: String,
    concurrency: Int = 1,
    groupStrategy: GroupStrategy = GroupStrategy.TimeWindow(10, 250.milliseconds),
    builder: PublishRequest.Builder.(T) -> Unit
): Flow<PublishResponse> =
    publishFlow(
        topicArn = topicArn,
        upstream = map { PublishRequest { builder(it) } },
        concurrency = concurrency,
        groupStrategy = groupStrategy
    )
