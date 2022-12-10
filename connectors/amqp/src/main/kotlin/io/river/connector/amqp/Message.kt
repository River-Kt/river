package io.river.connector.amqp

import com.rabbitmq.client.AMQP

sealed interface Message {
    val body: ByteArray
    val properties: AMQP.BasicProperties
    val mandatory: Boolean
    val immediate: Boolean

    data class Default(
        val exchange: String,
        val routingKey: String,
        override val body: ByteArray,
        override val properties: AMQP.BasicProperties = AMQP.BasicProperties(),
        override val mandatory: Boolean = false,
        override val immediate: Boolean = false
    ) : Message

    data class Simple(
        val body: ByteArray,
        val properties: AMQP.BasicProperties = AMQP.BasicProperties(),
        val mandatory: Boolean = false,
        val immediate: Boolean = false
    ) {
        fun asDefault(
            exchange: String,
            routingKey: String,
        ) = Default(exchange, routingKey, body, properties, mandatory, immediate)
    }
}
