package com.river.connector.amqp

import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.impl.nio.NioParams
import com.river.connector.amqp.internal.internalConsume
import com.river.core.ExperimentalRiverApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map

@ExperimentalRiverApi
fun nonBlockingConnectionFactory(
    threadNumber: Int = 1,
    f: ConnectionFactory.() -> Unit
): ConnectionFactory = ConnectionFactory().apply {
    useNio()
    nioParams = NioParams().setNbIoThreads(threadNumber)
}.also(f)

@ExperimentalRiverApi
fun <T> Connection.withChannel(f: Channel.() -> T): T =
    createChannel()
        .let { channel -> f(channel).also { channel.close() } }

@ExperimentalRiverApi
fun ConnectionFactory.connection(name: String? = null) =
    newConnection(name)

@ExperimentalCoroutinesApi
@ExperimentalRiverApi
fun Connection.autoAckConsume(
    queue: String,
    prefetch: Int = 100
): Flow<ReceivingMessage.AutoAck> =
    internalConsume(queue, true, prefetch).filterIsInstance()

@ExperimentalCoroutinesApi
@ExperimentalRiverApi
fun Connection.consume(
    queue: String,
    prefetch: Int = 100
): Flow<ReceivingMessage.ManualAck> =
    internalConsume(queue, false, prefetch).filterIsInstance()

@ExperimentalRiverApi
fun Connection.publishFlow(
    exchange: String,
    routingKey: String,
    upstream: Flow<Message.Simple>
) = createChannel()
    .publishFlow(exchange, routingKey, upstream)

@ExperimentalRiverApi
fun Channel.publishFlow(
    exchange: String,
    routingKey: String,
    upstream: Flow<Message.Simple>
) = upstream
    .map { it.asDefault(exchange, routingKey) }
    .let { publishFlow(it) }

@ExperimentalRiverApi
fun Channel.publishFlow(
    upstream: Flow<Message.Default>
) = upstream
    .map {
        basicPublish(it.exchange, it.routingKey, it.mandatory, it.immediate, it.properties, it.body)
    }
