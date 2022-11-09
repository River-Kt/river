package io.github.gabfssilva.river.aws.sqs

import software.amazon.awssdk.services.sqs.model.Message

data class MessageAcknowledgment<T : Acknowledgment>(
    val message: Message,
    val acknowledgment: T
)
