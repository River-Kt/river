package io.river.connector.aws.sqs.model

import software.amazon.awssdk.services.sqs.model.Message

data class MessageAcknowledgment<T : Acknowledgment>(
    val message: Message,
    val acknowledgment: T
)
