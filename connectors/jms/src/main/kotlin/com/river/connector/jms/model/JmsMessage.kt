package com.river.connector.jms.model

import java.io.Serializable
import javax.jms.JMSContext
import javax.jms.Message

sealed class JmsMessage(
    internal val message: JMSContext.() -> Message,
    val correlationId: String? = null,
    val replyTo: JmsDestination? = null,
    val deliveryMode: DeliveryMode = DeliveryMode.PERSISTENT,
    val properties: kotlin.collections.Map<String, JmsPrimitive<*>> = emptyMap()
) {
    internal val build: JMSContext.() -> Message = {
        val context = this

        message
            .invoke(context)
            .apply {
                jmsCorrelationID = correlationId
                jmsReplyTo = replyTo?.destination?.invoke(context)
                jmsDeliveryMode = deliveryMode.value
                properties.forEach { (k, v) -> setObjectProperty(k, v.value) }
            }
    }

    class Text(
        value: String,
        correlationId: String? = null,
        replyTo: JmsDestination? = null,
        deliveryMode: DeliveryMode = DeliveryMode.PERSISTENT,
        properties: kotlin.collections.Map<String, JmsPrimitive<*>> = emptyMap()
    ) : JmsMessage(
        message = { createTextMessage(value) },
        correlationId = correlationId,
        replyTo = replyTo,
        deliveryMode = deliveryMode,
        properties = properties
    )

    class Object(
        value: Serializable,
        correlationId: String? = null,
        replyTo: JmsDestination? = null,
        deliveryMode: DeliveryMode = DeliveryMode.PERSISTENT,
        properties: kotlin.collections.Map<String, JmsPrimitive<*>> = emptyMap()
    ) : JmsMessage(
        message = { createObjectMessage(value) },
        correlationId = correlationId,
        replyTo = replyTo,
        deliveryMode = deliveryMode,
        properties = properties
    )

    class Map(
        value: kotlin.collections.Map<String, JmsPrimitive<*>>,
        correlationId: String? = null,
        replyTo: JmsDestination? = null,
        deliveryMode: DeliveryMode = DeliveryMode.PERSISTENT,
        properties: kotlin.collections.Map<String, JmsPrimitive<*>> = emptyMap()
    ) : JmsMessage(
        message = { createMapMessage().apply { value.forEach { (t, u) -> setObject(t, u.value) } } },
        correlationId = correlationId,
        replyTo = replyTo,
        deliveryMode = deliveryMode,
        properties = properties
    )

    class Bytes(
        value: ByteArray,
        correlationId: String? = null,
        replyTo: JmsDestination? = null,
        deliveryMode: DeliveryMode = DeliveryMode.PERSISTENT,
        properties: kotlin.collections.Map<String, JmsPrimitive<*>> = emptyMap()
    ) : JmsMessage(
        message = { createBytesMessage().apply { writeBytes(value) } },
        correlationId = correlationId,
        replyTo = replyTo,
        deliveryMode = deliveryMode,
        properties = properties
    )
}
