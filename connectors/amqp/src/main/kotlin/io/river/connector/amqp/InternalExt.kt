package io.river.connector.amqp

import com.rabbitmq.client.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.channels.Channel as KotlinChannel

@OptIn(ExperimentalCoroutinesApi::class)
private val SingleThreadMessage: CoroutineDispatcher =
    Dispatchers.IO.limitedParallelism(1)

internal fun Connection.internalConsume(
    queue: String,
    autoAck: Boolean,
    prefetch: Int
): Flow<ReceivingMessage> {
    val amqpChannel = createChannel().apply { basicQos(prefetch, false) }
    val channel = KotlinChannel<ReceivingMessage>(capacity = prefetch)

    amqpChannel
        .basicConsume(
            queue = queue,
            autoAck = autoAck,
            handleCancelOk = {
                channel.close(IllegalStateException("Consumer $it is being cancelled by Channel.basicCancel"))
            },
            handleCancel = {
                channel.close(IllegalStateException("Consumer $it is being cancelled"))
            },
            handleShutdownSignal = { tag, e ->
                channel.close(e)
            }
        ) { consumerTag, envelope, properties, body ->
            runBlocking(SingleThreadMessage) {
                val message =
                    if (autoAck) {
                        ReceivingMessage.AutoAck(
                            consumerTag = consumerTag,
                            envelope = envelope,
                            properties = properties,
                            body = body,
                        )
                    } else {
                        ReceivingMessage.ManualAck(
                            consumerTag = consumerTag,
                            envelope = envelope,
                            properties = properties,
                            body = body,
                            channel = amqpChannel
                        )
                    }

                channel.send(message)
            }
        }

    return channel.consumeAsFlow()
}

private fun Channel.basicConsume(
    queue: String,
    autoAck: Boolean,
    handleCancelOk: (String) -> Unit = {},
    handleCancel: (String) -> Unit = {},
    handleShutdownSignal: (String, ShutdownSignalException?) -> Unit = { t, e -> },
    handleDelivery: (String, Envelope, AMQP.BasicProperties, ByteArray) -> Unit
) = basicConsume(queue, autoAck, object : DefaultConsumer(this) {
    override fun handleDelivery(
        consumerTag: String,
        envelope: Envelope,
        properties: AMQP.BasicProperties,
        body: ByteArray
    ) {
        handleDelivery(consumerTag, envelope, properties, body)
    }

    override fun handleCancelOk(consumerTag: String) {
        handleCancelOk(consumerTag)
    }

    override fun handleCancel(consumerTag: String) {
        handleCancel(consumerTag)
    }

    override fun handleShutdownSignal(consumerTag: String, sig: ShutdownSignalException) {
        handleShutdownSignal(consumerTag, sig)
    }
})
