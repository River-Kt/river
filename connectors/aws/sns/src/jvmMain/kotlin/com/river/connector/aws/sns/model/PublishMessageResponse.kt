package com.river.connector.aws.sns.model

import software.amazon.awssdk.services.sns.model.PublishBatchResponse

sealed interface PublishMessageResponse {
    val internalBatchResponse: PublishBatchResponse
    val id: String

    data class Successful(
        override val id: String,
        val messageId: String,
        val sequenceNumber: String?,
        override val internalBatchResponse: PublishBatchResponse
    ) : PublishMessageResponse

    data class Failure(
        override val id: String,
        val code: String,
        val message: String,
        val senderFault: Boolean,
        override val internalBatchResponse: PublishBatchResponse
    ) : PublishMessageResponse
}
