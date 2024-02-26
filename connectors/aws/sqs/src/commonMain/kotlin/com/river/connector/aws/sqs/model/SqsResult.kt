package com.river.connector.aws.sqs.model

import aws.sdk.kotlin.services.sqs.model.*
import com.river.connector.aws.sqs.model.Acknowledgment.ChangeMessageVisibility
import com.river.connector.aws.sqs.model.Acknowledgment.Delete

typealias MessageDeleteResult =
    SqsResult<MessageAcknowledgment<Delete>,  DeleteMessageBatchResultEntry,  BatchResultErrorEntry>

typealias ChangeMessageVisibilityResult =
    SqsResult<MessageAcknowledgment<ChangeMessageVisibility>,  ChangeMessageVisibilityBatchResultEntry,  BatchResultErrorEntry>

typealias MessageAcknowledgmentResult =
    SqsResult<MessageAcknowledgment<Acknowledgment>,  Any,  BatchResultErrorEntry>

typealias SendMessageResult =
    SqsResult<SendMessageRequest, SendMessageBatchResultEntry, BatchResultErrorEntry>

sealed interface SqsResult<out T, out S, out E> {
    val batchIndex: Int
    val request: T

    data class Success<T, S>(
        override val batchIndex: Int,
        override val request: T,
        val success: S,
    ) : SqsResult<T, S, Nothing>

    data class Failure<T, E>(
        override val batchIndex: Int,
        override val request: T,
        val error: E
    ) : SqsResult<T, Nothing, E>
}
