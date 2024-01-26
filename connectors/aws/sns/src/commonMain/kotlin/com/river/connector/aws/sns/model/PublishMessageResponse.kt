package com.river.connector.aws.sns.model

sealed interface PublishMessageResponse {
    val id: String

    data class Successful(
        override val id: String,
        val messageId: String,
        val sequenceNumber: String?,
    ) : PublishMessageResponse

    data class Failure(
        override val id: String,
        val code: String,
        val message: String,
        val senderFault: Boolean,
    ) : PublishMessageResponse
}
