package com.river.connector.aws.sns.internal

import com.river.connector.aws.sns.model.MessageAttributeValue
import com.river.connector.aws.sns.model.PublishMessageRequest
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.sns.model.PublishBatchRequestEntry
import software.amazon.awssdk.services.sns.model.MessageAttributeValue as SdkAttributeValue

internal fun PublishMessageRequest.asEntry(id: String): PublishBatchRequestEntry =
    PublishBatchRequestEntry
        .builder()
        .apply {
            message(message)

            messageAttributes(
                messageAttributes
                    .mapValues { (_, value) ->
                        when (value.type) {
                            MessageAttributeValue.Type.BINARY ->
                                SdkAttributeValue
                                    .builder()
                                    .dataType("binary")
                                    .binaryValue(SdkBytes.fromByteArray(value.data as ByteArray))
                                    .build()

                            else ->
                                SdkAttributeValue
                                    .builder()
                                    .dataType(value.type.name.lowercase())
                                    .stringValue("${value.data}")
                                    .build()
                        }
                    }
            )

            id(id)

            subject?.also { subject(it) }
            messageStructure?.also { messageStructure(it) }
            messageDeduplicationId?.also { messageDeduplicationId(it) }
            messageGroupId?.also { messageGroupId(it) }
        }
        .build()
