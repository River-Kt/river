package io.river.connector.jms

import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.shouldBe
import io.river.core.toList
import io.river.core.via
import kotlinx.coroutines.flow.*
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory
import javax.jms.ConnectionFactory
import javax.jms.TextMessage

class JmsExtKtTest : FeatureSpec({
    feature("JMS flow's ext") {
        withBroker {
            withConnectionFactory {
                scenario("Publish messages") {
                    (1..100)
                        .asFlow()
                        .map { JmsMessage.Text("hello, #$it!") }
                        .via { sendToDestination(JmsDestination.Queue("hello.world")) }
                        .count() shouldBe 100
                }

                scenario("Consume messages") {
                    (1..100)
                        .asFlow()
                        .map { JmsMessage.Text("hello, #$it!") }
                        .via { sendToDestination(JmsDestination.Queue("hello.world")) }
                        .collect()

                    val messages =
                        consume("hello.world")
                            .onEach { it.coAcknowledge() }
                            .filterIsInstance<TextMessage>()
                            .map { it.text }
                            .toList(100)

                    messages shouldContainInOrder (1..100).map { "hello, #$it!" }
                }
            }
        }
    }
})

inline fun withBroker(
    f: () -> Unit
) {
    val server =
        EmbeddedActiveMQ()
            .apply {
                configuration = ConfigurationImpl().apply {
                    addAcceptorConfiguration("in-vm", "vm://0")
                    addAcceptorConfiguration("tcp", "tcp://127.0.0.1:61616")
                    isSecurityEnabled = false
                }
            }
            .start()

    f()
    server.stop()
}

inline fun withConnectionFactory(
    f: ConnectionFactory.() -> Unit
) {
    f(ActiveMQConnectionFactory("tcp://localhost:61616"))
}
