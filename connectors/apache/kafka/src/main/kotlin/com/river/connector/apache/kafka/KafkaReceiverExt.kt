package com.river.connector.apache.kafka

import com.river.core.launchChannelConsumer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.asPublisher
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.apache.kafka.clients.consumer.ConsumerConfig.*
import reactor.kafka.receiver.KafkaReceiver
import reactor.kafka.receiver.ReceiverOffset
import reactor.kafka.receiver.ReceiverOptions
import reactor.kafka.receiver.ReceiverRecord
import reactor.kafka.sender.KafkaSender
import reactor.kafka.sender.SenderOptions
import reactor.kafka.sender.SenderRecord

fun <K, V> KafkaReceiver<K, V>.receiveAsFlow(): Flow<ReceiverRecord<K, V>> =
    receive().asFlow()

fun <K, V> kafkaReceiver(
    props: Map<String, Any> = emptyMap(),
    f: MutableReceiverOptions<K, V>.() -> Unit
): KafkaReceiver<K, V> = KafkaReceiver.create(receiverOptions(props, f))

fun <K, V> receiverOptions(
    props: Map<String, Any> = emptyMap(),
    f: MutableReceiverOptions<K, V>.() -> Unit
): ReceiverOptions<K, V> =
    MutableReceiverOptions<K, V>(props)
        .also(f)
        .toReceiverOptions()

fun <K, V> kafkaReceiverFlow(
    props: Map<String, Any> = emptyMap(),
    f: MutableReceiverOptions<K, V>.() -> Unit
): Flow<ReceiverRecord<K, V>> = kafkaReceiver(props, f).receiveAsFlow()

fun <K, V> senderOptions(
    props: Map<String, Any> = emptyMap(),
    f: MutableSenderOptions<K, V>.() -> Unit
): SenderOptions<K, V> =
    MutableSenderOptions<K, V>(props)
        .also(f)
        .toSenderOptions()

fun <K, V> kafkaSender(
    props: Map<String, Any> = emptyMap(),
    f: MutableSenderOptions<K, V>.() -> Unit
): KafkaSender<K, V> = KafkaSender.create(senderOptions(props, f))

fun <K, V, T> CoroutineScope.kafkaSenderChannel(
    props: Map<String, Any> = emptyMap(),
    f: MutableSenderOptions<K, V>.() -> Unit
): Channel<SenderRecord<K, V, T>> =
    launchChannelConsumer {
        val sender = kafkaSender(props, f)

        sender
            .send(receiveAsFlow())
            .collect()
    }

fun <K, V, T> KafkaSender<K, V>.send(
    upstream: Flow<SenderRecord<K, V, T>>
) = send(upstream.asPublisher()).asFlow()

suspend fun ReceiverOffset.coCommit() {
    commit().awaitFirstOrNull()
}

suspend fun ReceiverRecord<*, *>.coCommit() {
    receiverOffset().coCommit()
}

fun ReceiverRecord<*, *>.acknowledge() {
    receiverOffset().acknowledge()
}

suspend fun main(): Unit = coroutineScope {
    val recordFlow =
        kafkaReceiverFlow {
            properties {
                BOOTSTRAP_SERVERS_CONFIG to "localhost:29094"
                GROUP_ID_CONFIG to "sample-group6"
                AUTO_OFFSET_RESET_CONFIG to "earliest"
            }

            subscription { topics("sample") }
            keyDeserializer { _, data -> String(data) }
            valueDeserializer { _, data -> String(data) }
        }

//    val sender =
//        kafkaSenderChannel<String, String, String> {
//            properties {
//                BOOTSTRAP_SERVERS_CONFIG to "localhost:29094"
//                GROUP_ID_CONFIG to "sample-group3"
//                AUTO_OFFSET_RESET_CONFIG to "earliest"
//            }
//
//            keySerializer { _, data -> data.encodeToByteArray() }
//            valueSerializer { _, data -> data.encodeToByteArray() }
//        }
//
//    (1..100000)
//        .asFlow()
//        .map { create(ProducerRecord("sample", "$it", "$it"), "123") }
//        .collect { sender.send(it) }
//
//    sender.close()
//    println("done")

    recordFlow
        .collect {
            println(it.value())
            it.acknowledge()
        }

    println("done")
}
