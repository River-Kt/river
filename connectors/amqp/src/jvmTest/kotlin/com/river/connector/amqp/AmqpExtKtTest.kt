package com.river.connector.amqp

import com.rabbitmq.client.BuiltinExchangeType
import com.river.core.toList
import io.kotest.assertions.AssertionFailedError
import io.kotest.assertions.retry
import io.kotest.assertions.timing.eventually
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class AmqpExtKtTest : FeatureSpec({
    feature("AMQP flow") {
        val connection =
            nonBlockingConnectionFactory { setUri("amqp://admin:admin@localhost:5672") }
                .connection()

        beforeTest {
            connection.withChannel {
                queueDelete("hello.world")
                queueDeclare("hello.world", false, false, false, emptyMap())
                exchangeDeclare("hello", BuiltinExchangeType.DIRECT, true)
                queueBind("hello.world", "hello", "world")
            }
        }

        scenario("Basic publish flow") {
            (1..1000)
                .asFlow()
                .map { Message.Simple(it.toString().toByteArray()) }
                .let { connection.publishFlow("hello", "world", it) }
                .collect()

            eventually(duration = 1.seconds, poll = 100.milliseconds) {
                val count =
                    connection
                        .withChannel { queueDeclarePassive("hello.world").messageCount }

                count shouldBe 1000
            }
        }

        scenario("Basic message consuming") {
            (1..1000)
                .asFlow()
                .map { Message.Simple(it.toString().toByteArray()) }
                .let { connection.publishFlow("hello", "world", it) }
                .collect()

            val numbers = connection
                .consume("hello.world")
                .map {
                    it.ack()
                    it.bodyAsString().toInt()
                }
                .toList(1000)

            numbers shouldContainAll (1..1000).toList()

            val count =
                connection.withChannel { queueDeclarePassive("hello.world").messageCount }

            count shouldBe 0
        }
    }
})
