@file:OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)

package io.river.connector.jms

import io.river.connector.jms.model.*
import io.river.core.flatten
import io.river.core.mapParallel
import io.river.core.repeat
import io.river.core.unorderedMapParallel
import io.river.util.pool.ObjectPool
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

fun ConnectionFactory.sendToDestination(
    destination: JmsDestination,
    upstream: Flow<JmsMessage>,
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

        upstream
            .mapParallel(parallelism) { send(it) }
            .collect { emit(Unit) }
    }
}
