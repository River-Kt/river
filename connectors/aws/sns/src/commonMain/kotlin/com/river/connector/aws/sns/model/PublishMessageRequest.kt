package com.river.connector.aws.sns.model

data class PublishMessageRequest(
    val message: String,
    val subject: String? = null,
    val messageStructure: String? = null,
    val messageDeduplicationId: String? = null,
    val messageGroupId: String? = null,
    val messageAttributes: Map<String, MessageAttributeValue<*>> = emptyMap(),
)

class MessageAttributeValue<T> private constructor(
    val type: Type,
    val data: T
) {
    enum class Type { STRING, NUMBER, BINARY }

    companion object {
        fun string(data: String) = MessageAttributeValue(Type.STRING, data)
        fun number(data: Number) = MessageAttributeValue(Type.NUMBER, data)
        fun binary(data: ByteArray) = MessageAttributeValue(Type.BINARY, data)
    }
}
