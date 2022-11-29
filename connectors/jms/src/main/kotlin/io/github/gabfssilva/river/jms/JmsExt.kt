@file:OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)

package io.github.gabfssilva.river.jms

import io.github.gabfssilva.river.core.*
import io.github.gabfssilva.river.util.pool.ObjectPool
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.invoke
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory
import java.io.Serializable
import javax.jms.ConnectionFactory
import javax.jms.JMSContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

private fun ConnectionFactory.newBlockingContext(
    sessionMode: SessionMode = SessionMode.CLIENT_ACKNOWLEDGE,
    credentials: Credentials? = null,
) = credentials
    ?.let { createContext(it.username, it.password, sessionMode.value) }
    ?: createContext(sessionMode.value)

fun ConnectionFactory.consume(
    queueName: String,
    credentials: Credentials? = null,
    sessionMode: SessionMode = SessionMode.CLIENT_ACKNOWLEDGE,
    pollingMaxWait: Duration = 10.seconds,
    parallelism: Int = 1
): Flow<CommitableMessage> {
    val IO: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(parallelism)

    suspend fun newContext(): JMSContext =
        IO { newBlockingContext(sessionMode, credentials) }

    return flow {
        val queue = newContext().use { it.createQueue(queueName) }

        val contextPool =
            ObjectPool.sized(
                maxSize = parallelism,
                onClose = { (context, consumer) -> IO { consumer.close(); context.close() } },
                factory = { IO { newContext().let { it to it.createConsumer(queue) } } }
            )

        emitAll(
            repeat(contextPool)
                .unorderedMapParallel(parallelism) {
                    val instance = it.borrow()
                    val (_, consumer) = instance.instance

                    IO {
                        consumer.receive(pollingMaxWait.inWholeMilliseconds)?.let { message ->
                            CommitableMessage(message) {
                                IO { message.acknowledge() }
                                it.release(instance)
                            }
                        }?.let { listOf(it) } ?: emptyList()
                    }
                }
                .flatten()
                .onCompletion { contextPool.close() }
        )
    }
}

context(Flow<JmsMessage>)
fun ConnectionFactory.sendToDestination(
    destination: JmsDestination,
    parallelism: Int = 1,
    credentials: Credentials? = null,
): Flow<Unit> {
    val IO = Dispatchers.IO.limitedParallelism(parallelism)

    suspend fun newContext(): JMSContext =
        IO { newBlockingContext(credentials = credentials) }

    return flow {
        val contextPool =
            ObjectPool.sized(
                maxSize = parallelism,
                onClose = { (context, _) -> IO { context.close() } },
                factory = { IO { newContext().let { it to it.createProducer() } } }
            )

        val dest = newContext().use { destination.destination(it) }

        suspend fun send(message: JmsMessage) =
            contextPool
                .use { (context, producer) ->
                    IO { producer.send(dest, message.build(context)) }
                }

        mapParallel(parallelism) { send(it) }
            .collect { emit(Unit) }
    }
}

fun ConnectionFactory.sendToDestination(
    destination: JmsDestination,
    upstream: Flow<JmsMessage>,
    parallelism: Int = 1
): Flow<Unit> = with(upstream) { sendToDestination(destination, parallelism) }

suspend fun main() {
    val server = EmbeddedActiveMQ().apply {
        configuration = ConfigurationImpl().apply {
            addAcceptorConfiguration("in-vm", "vm://0")
            addAcceptorConfiguration("tcp", "tcp://127.0.0.1:61616")
            isSecurityEnabled = false
        }
    }

    server.start()

    data class Greeting(
        val greeting: String,
        val name: String
    ) : Serializable

    with(ActiveMQConnectionFactory("tcp://localhost:61616")) {
        measureTimedValue {
            (0..10000)
                .asFlow()
                .map { Greeting(if (it % 2 == 0) "hello" else "hi", "#$it") }
                .map {
                    JmsMessage.Object(
                        value = it,
                        correlationId = it.name,
                        properties = mapOf(
                            "name" to JmsPrimitive.Text(it.name)
                        )
                    )
                }
                .via { sendToDestination(JmsDestination.Queue("hello"), 100) }
                .collect()
        }.let {
            println("done within ${it.duration}")
        }

        kotlin.runCatching {
            consume(queueName = "hello", sessionMode = SessionMode.CLIENT_ACKNOWLEDGE, parallelism = 1)
                .filterIsInstance<CommitableMessage.CommitableObjectMessage>()
                .collect {
                    val greeting = it.`object` as Greeting
                    println("$greeting - ${it.jmsCorrelationID} - ${it.getStringProperty("name")}")
                    it.coAcknowledge()
                }
        }.getOrElse {
            println("ué")
            it.printStackTrace()
        }
    }
}
