package io.river.connector.amqp

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Envelope
import java.nio.charset.Charset

sealed interface ReceivingMessage {
    val consumerTag: String
    val envelope: Envelope
    val properties: AMQP.BasicProperties
    val body: ByteArray

    data class AutoAck(
        override val consumerTag: String,
        override val envelope: Envelope,
        override val properties: AMQP.BasicProperties,
        override val body: ByteArray
    ) : ReceivingMessage

    data class ManualAck(
        override val consumerTag: String,
        override val envelope: Envelope,
        override val properties: AMQP.BasicProperties,
        override val body: ByteArray,
        private val channel: Channel,
    ) : ReceivingMessage {
        fun ack() = channel.basicAck(envelope.deliveryTag, false)

        fun nack(
            requeue: Boolean = true,
        ) = channel.basicNack(envelope.deliveryTag, false, requeue)
    }

    fun bodyAsString(
        charset: Charset = Charset.defaultCharset()
    ) = String(body, charset)
}
