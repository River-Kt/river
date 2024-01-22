package com.river.connector.aws.sns.model

import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishBatchRequestEntry

data class PublishMessageRequest(
    val message: String,
    val subject: String? = null,
    val messageStructure: String? = null,
    val messageDeduplicationId: String? = null,
    val messageGroupId: String? = null,
    val messageAttributes: Map<String, MessageAttributeValue> = emptyMap(),
) {
    internal fun asEntry(id: String): PublishBatchRequestEntry =
        PublishBatchRequestEntry
            .builder()
            .apply {
                message(message)
                messageAttributes(messageAttributes)
                id(id)

                subject?.also { subject(it) }
                messageStructure?.also { messageStructure(it) }
                messageDeduplicationId?.also { messageDeduplicationId(it) }
                messageGroupId?.also { messageGroupId(it) }
            }
            .build()
}
