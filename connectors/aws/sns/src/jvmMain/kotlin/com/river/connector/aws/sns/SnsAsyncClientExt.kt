package com.river.connector.aws.sns

import com.river.connector.aws.sns.model.PublishMessageRequest
import com.river.connector.aws.sns.model.PublishMessageResponse
import com.river.core.GroupStrategy
import kotlinx.coroutines.flow.Flow
import software.amazon.awssdk.services.sns.SnsAsyncClient
import kotlin.time.Duration.Companion.milliseconds

/**
 * The [publishFlow] function is used to publish messages concurrently to an Amazon Simple Notification Service (SNS)
 * topic using a [SnsAsyncClient].
 *
 * It takes an input [Flow] of [PublishMessageRequest] and publishes the messages
 * concurrently, respecting the specified [groupStrategy].
 *
 * The [groupStrategy] parameter defines how the messages should be grouped. Using a time window, for instance, the messages
 * are going to be grouped either when the maximum number of items is reached, or when the time duration has elapsed, whichever
 * comes first. This can help balance between processing latency and the granularity of data aggregation, leading
 * to more efficient processing by potentially reducing the number of requests to AWS.
 *
 * @param upstream The input [Flow] of [PublishMessageRequest]s to be published.
 * @param concurrency The degree of concurrency to use for publishing messages (default is 1).
 * @param groupStrategy The [GroupStrategy] to use for grouping the messages (default is TimeWindow with 10 items and 250 milliseconds).
 * @param topicArn A lambda function that returns the Amazon Resource Name (ARN) of the SNS topic to publish the messages to.

 * @return A [Flow] of [PublishMessageResponse]s for the published messages.
 *
 * Example usage:
 * ```
 * val snsClient: SnsAsyncClient = ...
 * val messages: Flow<PublishMessageRequest> = ...
 *
 * val responses: Flow<PublishMessageResponse> = snsClient.publishFlow(topicArn, messages) {
 *     "arn:aws:sns:us-east-1:123456789012:MyTopic"
 * }
 *
 * responses.collect { response ->
 *     when (response) {
 *         is Successful -> println("Message ${response.id} successfully published with messageId: ${response.messageId}")
 *         is Failure -> println("Message ${response.id} failed to publish with error code: ${response.code}")
 *     }
 * }
 * ```
 */
fun SnsAsyncClient.publishFlow(
    upstream: Flow<PublishMessageRequest>,
    concurrency: Int = 1,
    groupStrategy: GroupStrategy = GroupStrategy.TimeWindow(10, 250.milliseconds),
    topicArn: suspend () -> String
): Flow<PublishMessageResponse> =
    SnsClient(this)
        .publishFlow(upstream, concurrency, groupStrategy, topicArn)
