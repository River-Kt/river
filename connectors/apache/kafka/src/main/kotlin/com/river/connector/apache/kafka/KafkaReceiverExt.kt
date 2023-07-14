package com.river.connector.apache.kafka

import com.river.core.launchChannelConsumer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.asPublisher
import kotlinx.coroutines.reactive.awaitFirstOrNull
import reactor.kafka.receiver.KafkaReceiver
import reactor.kafka.receiver.ReceiverOffset
import reactor.kafka.receiver.ReceiverOptions
import reactor.kafka.receiver.ReceiverRecord
import reactor.kafka.sender.KafkaSender
import reactor.kafka.sender.SenderOptions
import reactor.kafka.sender.SenderRecord
import reactor.kafka.sender.SenderResult

/**
 * This function is an extension function for KafkaReceiver that returns a Flow of ReceiverRecords.
 *
 * @return a [Flow] of [ReceiverRecord].
 *
 * Example usage:
 *
 * ```kotlin
 * val kafkaReceiver: KafkaReceiver<String, String> = ...
 *
 * val flow: Flow<ReceiverRecord<String, String>> = kafkaReceiver.receiveAsFlow()
 * ```
 */
fun <K, V> KafkaReceiver<K, V>.receiveAsFlow(): Flow<ReceiverRecord<K, V>> =
    receive().asFlow()

/**
 * This function creates a [KafkaReceiver] using provided receiver options.
 *
 * @param options A lambda with receiver to configure [MutableReceiverOptions].
 *
 * @return a [KafkaReceiver].
 *
 * Example usage:
 *
 * ```kotlin
 * val receiver = kafkaReceiver<String, String> {
 *     keyDeserializer { _, data -> String(data) }
 *     valueDeserializer { _, data -> String(data) }
 *
 *     properties {
 *         BOOTSTRAP_SERVERS_CONFIG to "localhost:9092"
 *         GROUP_ID_CONFIG to "my-group-id"
 *         AUTO_OFFSET_RESET_CONFIG to "earliest"
 *     }
 * }
 * ```
 */
fun <K, V> kafkaReceiver(
    options: MutableReceiverOptions<K, V>.() -> Unit
): KafkaReceiver<K, V> = KafkaReceiver.create(receiverOptions(options))

/**
 * This function provides [ReceiverOptions] using provided options.
 *
 * @param options A lambda with receiver to configure [MutableReceiverOptions].
 * @return [ReceiverOptions].
 *
 * Example usage:
 * ```kotlin
 * val receiverOptions = receiverOptions<String, String> {
 *     keyDeserializer { _, data -> String(data) }
 *     valueDeserializer { _, data -> String(data) }
 *
 *     properties {
 *         BOOTSTRAP_SERVERS_CONFIG to "localhost:9092"
 *         GROUP_ID_CONFIG to "my-group-id"
 *         AUTO_OFFSET_RESET_CONFIG to "earliest"
 *     }
 * }
 * ```
 */
fun <K, V> receiverOptions(
    options: MutableReceiverOptions<K, V>.() -> Unit
): ReceiverOptions<K, V> =
    MutableReceiverOptions<K, V>()
        .also(options)
        .toReceiverOptions()

/**
 * This function creates a KafkaReceiver and converts it into a [Flow] using provided options.
 *
 * @param options A lambda with receiver to configure [MutableReceiverOptions].
 *
 * @return a [Flow] of [ReceiverRecord].
 *
 * Example usage:
 *
 * ```kotlin
 * val flow = kafkaReceiverFlow<String, String> {
 *     keyDeserializer { _, data -> String(data) }
 *     valueDeserializer { _, data -> String(data) }
 *
 *     properties {
 *         BOOTSTRAP_SERVERS_CONFIG to "localhost:9092"
 *         GROUP_ID_CONFIG to "my-group-id"
 *         AUTO_OFFSET_RESET_CONFIG to "earliest"
 *     }
 * }
 * flow.collect { record -> println(record.value()) }
 * ```
 */
fun <K, V> kafkaReceiverFlow(
    options: MutableReceiverOptions<K, V>.() -> Unit
): Flow<ReceiverRecord<K, V>> = kafkaReceiver(options).receiveAsFlow()

/**
 * This function creates [SenderOptions] using the provided configuration block.
 *
 * @param options A lambda with receiver to configure [MutableSenderOptions].
 *
 * @return [SenderOptions].
 *
 * Example usage:
 * ```kotlin
 * val senderOptions = senderOptions<String, String> {
 *     keySerializer { _, data -> data.encodeToByteArray() }
 *     valueSerializer { _, data -> data.encodeToByteArray() }
 *
 *     properties {
 *         BOOTSTRAP_SERVERS_CONFIG to "localhost:9092"
 *     }
 * }
 * ```
 */
fun <K, V> senderOptions(
    options: MutableSenderOptions<K, V>.() -> Unit
): SenderOptions<K, V> =
    MutableSenderOptions<K, V>()
        .also(options)
        .toSenderOptions()

/**
 * This function creates a [KafkaSender] using provided sender options.
 *
 * @param options A lambda with receiver to configure [MutableSenderOptions].
 * @return a [KafkaSender].
 *
 * Example usage:
 * ```kotlin
 * val sender = kafkaSender<String, String> {
 *     keySerializer { _, data -> data.encodeToByteArray() }
 *     valueSerializer { _, data -> data.encodeToByteArray() }
 *
 *     properties {
 *         BOOTSTRAP_SERVERS_CONFIG to "localhost:9092"
 *     }
 * }
 * ```
 */
fun <K, V> kafkaSender(
    options: MutableSenderOptions<K, V>.() -> Unit
): KafkaSender<K, V> = KafkaSender.create(senderOptions(options))

/**
 * This function creates a [Channel] for [SenderRecord] in the [CoroutineScope] using provided options.
 *
 * @param options A lambda with receiver to configure [MutableSenderOptions].
 *
 * @return a [Channel] of [SenderRecord].
 *
 * Example usage:
 * ```kotlin
 * coroutineScope {
 *     val senderChannel = kafkaSenderChannel<String, String, Unit> {
 *         keySerializer { _, data -> data.encodeToByteArray() }
 *         valueSerializer { _, data -> data.encodeToByteArray() }
 *
 *         properties {
 *             BOOTSTRAP_SERVERS_CONFIG to "localhost:9092"
 *         }
 *     }
 *
 *     senderChannel.send(SenderRecord.create(record, Unit))
 * }
 * ```
 */
fun <K, V, T> CoroutineScope.kafkaSenderChannel(
    options: MutableSenderOptions<K, V>.() -> Unit
): Channel<SenderRecord<K, V, T>> =
    launchChannelConsumer {
        val sender = kafkaSender(options)

        sender
            .send(receiveAsFlow())
            .collect()
    }

/**
 * This function sends records upstream as a [Flow] using the [KafkaSender].
 *
 * @param upstream The [Flow] of [SenderRecord] to be sent.
 *
 * @return a [Flow] of [SendResult].
 *
 * Example usage:
 * ```kotlin
 * val sender: KafkaSender<String, String> = ...
 * val upstream: Flow<SenderRecord<String, String, Unit>> = ...
 * val resultFlow: Flow<SendResult<String, String>> = sender.send(upstream)
 * ```
 */
fun <K, V, T> KafkaSender<K, V>.send(
    upstream: Flow<SenderRecord<K, V, T>>
): Flow<SenderResult<T>> = send(upstream.asPublisher()).asFlow()

/**
 * This function commits the [ReceiverOffset] suspending the coroutine until the commit operation is complete.
 *
 * Example usage:
 *
 * ```kotlin
 * val flow = kafkaReceiverFlow<String, String> { ... }
 *
 * flow.collect { record -> record.receiverOffset().acknowledge() }
 * ```
 */
suspend fun ReceiverOffset.coCommit() {
    commit().awaitFirstOrNull()
}


/**
 * This function commits the [ReceiverOffset] of the [ReceiverRecord] suspending the coroutine until the commit operation is complete.
 *
 * Example usage:
 * ```kotlin
 * val flow = kafkaReceiverFlow<String, String> { ... }
 *
 * flow.collect { record -> record.coCommit() }
 * ```
 */
suspend fun ReceiverRecord<*, *>.coCommit() {
    receiverOffset().coCommit()
}

/**
 * This function acknowledges the processing of the [ReceiverRecord] by committing its offset.
 *
 * Example usage:
 * ```kotlin
 * val flow = kafkaReceiverFlow<String, String> { ... }
 *
 * flow.collect { record -> record.acknowledge() }
 * ```
 */
fun ReceiverRecord<*, *>.acknowledge() {
    receiverOffset().acknowledge()
}
