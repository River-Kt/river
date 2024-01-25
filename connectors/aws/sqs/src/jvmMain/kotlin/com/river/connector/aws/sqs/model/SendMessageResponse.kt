package com.river.connector.aws.sqs.model

import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse

sealed interface SendMessageResponse {
    val internalBatchResponse: SendMessageBatchResponse
    val id: String

    data class Successful(
        override val id: String,
        val messageId: String,
        val sequenceNumber: String?,
        val md5OfMessageBody: String,
        val md5OfMessageAttributes: String?,
        val md5OfMessageSystemAttributes: String?,
        override val internalBatchResponse: SendMessageBatchResponse
    ) : SendMessageResponse

    data class Failure(
        override val id: String,
        val code: String,
        val message: String,
        val senderFault: Boolean,
        override val internalBatchResponse: SendMessageBatchResponse
    ) : SendMessageResponse
}
