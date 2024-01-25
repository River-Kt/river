package com.river.connector.apache.kafka

import com.river.core.countOnWindow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.*
import org.apache.kafka.clients.admin.Admin
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.consumer.ConsumerConfig.*
import org.apache.kafka.clients.producer.ProducerRecord
import reactor.kafka.sender.SenderRecord.create
import java.util.*
import kotlin.time.Duration.Companion.seconds

class KafkaReceiverExtKtTest : FunSpec({
    test("Assert that send is properly sending messages from a Flow of records") {
        val topicName = "${UUID.randomUUID()}"
        val consumerGroup = "${UUID.randomUUID()}"

        createTopic(topicName)

        val records =
            (1..1000)
                .asFlow()
                .map { create(ProducerRecord(topicName, "$it", "hello, this is the message #$it"), "123") }

        sender
            .send(records)
            .collect()

        val count =
            receiverFlow(topicName, consumerGroup)
                .countOnWindow(5.seconds)

        count shouldBeExactly 1000
    }

    test("Assert that kafkaReceiverFlow is properly consuming messages") {
        val topicName = "${UUID.randomUUID()}"
        val consumerGroup = "${UUID.randomUUID()}"

        createTopic(topicName)

        val records =
            (1..1000)
                .asFlow()
                .map { create(ProducerRecord(topicName, "$it", "hello, this is the message #$it"), "123") }

        sender
            .send(records)
            .collect()

        receiverFlow(topicName, consumerGroup)
            .take(1000)
            .collectIndexed { index, value ->
                value.key() shouldBe "${index + 1}"
                value.value() shouldBe "hello, this is the message #${index + 1}"
            }
    }

    test("Assert that kafkaReceiverFlow is idempotent consuming messages if offset reset is set to earliest and messages are not committed") {
        val topicName = "${UUID.randomUUID()}"
        val consumerGroup = "${UUID.randomUUID()}"

        createTopic(topicName)

        val records =
            (1..1000)
                .asFlow()
                .map { create(ProducerRecord(topicName, "$it", "hello, this is the message #$it"), "123") }

        sender
            .send(records)
            .collect()

        val receiverFlow = receiverFlow(topicName, consumerGroup)

        repeat(3) {
            val count = receiverFlow.countOnWindow(5.seconds)
            count shouldBeExactly 1000
        }
    }

    test("Assert that acknowledge is properly working") {
        val topicName = "${UUID.randomUUID()}"
        val consumerGroup = "${UUID.randomUUID()}"

        createTopic(topicName)

        val records =
            (1..1000)
                .asFlow()
                .map { create(ProducerRecord(topicName, "$it", "hello, this is the message #$it"), "123") }

        sender
            .send(records)
            .collect()

        val receiverFlow = receiverFlow(topicName, consumerGroup)

        receiverFlow
            .onEach { it.acknowledge() }
            .take(1000)
            .collect()

        val count = receiverFlow.countOnWindow(5.seconds)
        count shouldBeExactly 0
    }
})

val sender = kafkaSender<String, String> {
    properties {
        BOOTSTRAP_SERVERS_CONFIG to "localhost:29094"
        AUTO_OFFSET_RESET_CONFIG to "earliest"
    }

    keySerializer { _, data -> data.encodeToByteArray() }
    valueSerializer { _, data -> data.encodeToByteArray() }
}

fun receiverFlow(
    topic: String,
    consumerGroup: String
) = kafkaReceiverFlow {
    subscription { topics(topic) }

    keyDeserializer { _, data -> String(data) }
    valueDeserializer { _, data -> String(data) }

    properties {
        BOOTSTRAP_SERVERS_CONFIG to "localhost:29094"
        GROUP_ID_CONFIG to consumerGroup
        AUTO_OFFSET_RESET_CONFIG to "earliest"
    }
}

val kafkaAdmin = Admin.create(
    mapOf(
        BOOTSTRAP_SERVERS_CONFIG to "localhost:29094",
        GROUP_ID_CONFIG to "${UUID.randomUUID()}",
        AUTO_OFFSET_RESET_CONFIG to "earliest"
    )
)

fun createTopic(name: String) =
    kafkaAdmin.createTopics(listOf(NewTopic(name, 1, 1)))
