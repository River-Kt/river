package com.river.connector.aws.sqs.model

import software.amazon.awssdk.services.sqs.model.MessageAttributeValue
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry
import java.util.*

data class SendMessageRequest(
    val body: String,
    val delaySeconds: Int = 0,
    val messageAttributes: Map<String, MessageAttributeValue> = emptyMap(),
    val id: String = UUID.randomUUID().toString()
) {
    internal fun asMessageRequestEntry() =
        SendMessageBatchRequestEntry
            .builder()
            .apply {
                messageBody(body)
                delaySeconds(delaySeconds)
                messageAttributes(messageAttributes)
                id(id)
            }
            .build()
}
