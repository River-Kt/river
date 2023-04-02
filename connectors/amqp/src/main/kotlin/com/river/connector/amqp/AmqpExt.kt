package com.river.connector.amqp

import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.impl.nio.NioParams
import kotlinx.coroutines.flow.*

fun nonBlockingConnectionFactory(
    threadNumber: Int = 1,
    f: ConnectionFactory.() -> Unit
): ConnectionFactory = ConnectionFactory().apply {
    useNio()
    nioParams = NioParams().setNbIoThreads(threadNumber)
}.also(f)

fun <T> Connection.withChannel(f: Channel.() -> T): T =
    createChannel()
        .let { channel -> f(channel).also { channel.close() } }

fun ConnectionFactory.connection(name: String? = null) =
    newConnection(name)

fun Connection.autoAckConsume(
    queue: String,
    prefetch: Int = 100
): Flow<ReceivingMessage.AutoAck> =
    internalConsume(queue, true, prefetch).filterIsInstance()

fun Connection.consume(
    queue: String,
    prefetch: Int = 100
): Flow<ReceivingMessage.ManualAck> =
    internalConsume(queue, false, prefetch).filterIsInstance()

fun Connection.publishFlow(
    exchange: String,
    routingKey: String,
    upstream: Flow<Message.Simple>
) = createChannel()
    .publishFlow(exchange, routingKey, upstream)

fun Channel.publishFlow(
    exchange: String,
    routingKey: String,
    upstream: Flow<Message.Simple>
) = upstream
    .map { it.asDefault(exchange, routingKey) }
    .let { publishFlow(it) }

fun Channel.publishFlow(
    upstream: Flow<Message.Default>
) = upstream
    .map {
        basicPublish(it.exchange, it.routingKey, it.mandatory, it.immediate, it.properties, it.body)
    }
