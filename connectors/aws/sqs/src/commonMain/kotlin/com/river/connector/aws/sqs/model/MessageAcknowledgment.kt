package com.river.connector.aws.sqs.model

import aws.sdk.kotlin.services.sqs.model.Message


data class MessageAcknowledgment<out T : Acknowledgment>(
    val message: Message,
    val acknowledgment: T
)
