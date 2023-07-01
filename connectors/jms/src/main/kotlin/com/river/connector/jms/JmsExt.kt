@file:OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)

package com.river.connector.jms

import com.river.connector.jms.model.*
import com.river.core.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.invoke
import javax.jms.ConnectionFactory
import javax.jms.JMSContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

/**
 * Consumes messages from a specified JMS queue using a reactive Flow API.
 *
 * @param queueName The name of the queue to consume messages from.
 * @param credentials Optional credentials to use for establishing the connection. Defaults to null.
 * @param sessionMode The session mode for the JMS context. Defaults to SessionMode.CLIENT_ACKNOWLEDGE.
 * @param pollingMaxWait The maximum duration to wait for a message during polling. Defaults to 10 seconds.
 * @param concurrency The number of concurrent consumers for message consumption. Defaults to 1.
 *
 * @return A flow of [CommittableMessage] objects, which can be acknowledged after processing.
 *
 * Example usage:
 *
 * ```
 *  val connectionFactory = // Obtain a JMS connection factory
 *  val queueName = "example-queue"
 *
 *  connectionFactory.consume(queueName).collect { message ->
 *      println("Received message: ${message.message}")
 *
 *      // Process the message here
 *
 *      // Acknowledge the message after processing using coAcknowledge
 *      // Don't call acknowledge directly since it may perform blocking I/O
 *      message.coAcknowledge()
 *  }
 * ```
 */
fun ConnectionFactory.consume(
    queueName: String,
    credentials: Credentials? = null,
    sessionMode: SessionMode = SessionMode.CLIENT_ACKNOWLEDGE,
    pollingMaxWait: Duration = 10.seconds,
    concurrency: Int = 1
): Flow<CommittableMessage> {
    val IO: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(concurrency)

    suspend fun newContext(): JMSContext =
        IO { newBlockingContext(sessionMode, credentials) }

    return flow {
        val queue = newContext().use { it.createQueue(queueName) }

        val contextPool =
            objectPool(
                maxSize = concurrency,
                onClose = { (context, consumer) -> IO { consumer.close(); context.close() } },
                factory = { IO { newContext().let { it to it.createConsumer(queue) } } }
            )

        emitAll(
            indefinitelyRepeat(contextPool)
                .unorderedMapAsync(concurrency) {
                    val instance = it.borrow()
                    val (_, consumer) = instance.instance

                    IO {
                        consumer.receive(pollingMaxWait.inWholeMilliseconds)?.let { message ->
                            CommittableMessage(message) {
                                IO { message.acknowledge() }
                                it.release(instance)
                            }
                        }?.let { listOf(it) } ?: emptyList()
                    }
                }
                .flattenIterable()
                .onCompletion { contextPool.close() }
        )
    }
}

/**
 * Sends messages to a specified JMS destination from a given upstream flow of JmsMessage objects.
 *
 * @param destination The JmsDestination to send messages to.
 * @param upstream A flow of JmsMessage objects to be sent to the destination.
 * @param concurrency The number of concurrent producers for sending messages. Defaults to 1.
 * @param credentials Optional credentials to use for establishing the connection. Defaults to null.
 *
 * @return A flow of Unit objects, indicating that a message has been sent.
 *
 * Example usage:
 *
 * ```
 *  val connectionFactory = // Obtain a JMS connection factory
 *  val destination = JmsQueue("example-queue")
 *  val messagesFlow = flowOf(
 *      JmsTextMessage("Hello, River!"),
 *      JmsTextMessage("Just go with the flow")
 *  )
 *
 *  connectionFactory.sendToDestination(destination, messagesFlow)
 *      .collect { println("Message sent") }
 */
fun ConnectionFactory.sendToDestination(
    destination: JmsDestination,
    upstream: Flow<JmsMessage>,
    concurrency: Int = 1,
    credentials: Credentials? = null,
): Flow<Unit> {
    val IO = Dispatchers.IO.limitedParallelism(concurrency)

    suspend fun newContext(): JMSContext =
        IO { newBlockingContext(credentials = credentials) }

    return flow {
        val contextPool =
            objectPool(
                maxSize = concurrency,
                onClose = { (context, _) -> IO { context.close() } },
                factory = { IO { newContext().let { it to it.createProducer() } } }
            )

        val dest = newContext().use { destination.destination(it) }

        suspend fun send(message: JmsMessage) =
            contextPool
                .borrow { (context, producer) ->
                    IO { producer.send(dest, message.build(context)) }
                }

        upstream
            .mapAsync(concurrency) { send(it) }
            .collect { emit(Unit) }
    }
}

private fun ConnectionFactory.newBlockingContext(
    sessionMode: SessionMode = SessionMode.CLIENT_ACKNOWLEDGE,
    credentials: Credentials? = null,
) = credentials
    ?.let { createContext(it.username, it.password, sessionMode.value) }
    ?: createContext(sessionMode.value)
