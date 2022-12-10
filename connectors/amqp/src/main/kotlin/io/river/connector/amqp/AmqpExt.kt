package io.river.connector.amqp

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

fun <T> Connection.channel(f: Channel.() -> T): T =
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

context(Flow<Message.Default>)
fun Connection.publishFlow() =
    createChannel()
        .publishFlow()

context(Flow<Message.Default>)
fun Channel.publishFlow() = map {
    basicPublish(it.exchange, it.routingKey, it.mandatory, it.immediate, it.properties, it.body)
}

context(Flow<Message.Simple>)
fun Channel.publishFlow(
    exchange: String,
    routingKey: String
) = with(map { it.asDefault(exchange, routingKey) }) { publishFlow() }

context(Flow<Message.Simple>)
fun Connection.publishFlow(
    exchange: String,
    routingKey: String
) = with(map { it.asDefault(exchange, routingKey) }) { publishFlow() }

fun Connection.publishFlow(
    upstream: Flow<Message.Default>
) = with(upstream) { publishFlow() }

fun Channel.publishFlow(
    upstream: Flow<Message.Default>
) = with(upstream) { publishFlow() }

fun Connection.publishFlow(
    upstream: Flow<Message.Simple>,
    exchange: String,
    routingKey: String
) = with(upstream) { publishFlow(exchange, routingKey) }

fun Channel.publishFlow(
    upstream: Flow<Message.Simple>,
    exchange: String,
    routingKey: String
) = with(upstream) { publishFlow(exchange, routingKey) }
