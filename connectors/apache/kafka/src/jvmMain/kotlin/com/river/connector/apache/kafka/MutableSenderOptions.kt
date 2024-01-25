package com.river.connector.apache.kafka

import org.apache.kafka.clients.producer.Producer
import reactor.core.scheduler.Scheduler
import reactor.kafka.sender.SenderOptions
import kotlin.time.Duration
import kotlin.time.toJavaDuration

class MutableSenderOptions<K, V>(props: Map<String, Any> = mutableMapOf()) {
    private var underlying: SenderOptions<K, V> = SenderOptions.create(props)

    fun properties(builder: PropertyBuilder. () -> Unit) {
        PropertyBuilder()
            .also(builder)
            .properties()
            .forEach { (key, value) -> mutate { producerProperty(key, value) } }
    }

    fun keySerializer(transform: (topic: String, key: K) -> ByteArray) = mutate { withKeySerializer(transform) }
    fun valueSerializer(transform: (topic: String, value: V) -> ByteArray) = mutate { withValueSerializer(transform) }
    fun scheduler(scheduler: () -> Scheduler) = mutate { scheduler(scheduler()) }
    fun stopOnError(stopOnError: Boolean = true) = mutate { stopOnError(stopOnError) }
    fun maxInFlight(maxInFlight: Int) = mutate { maxInFlight(maxInFlight) }
    fun closeTimeout(closeTimeout: Duration) { mutate { closeTimeout(closeTimeout.toJavaDuration()) } }

    fun listener(
        added: (id: String, producer: Producer<*, *>) -> Unit = { _, _ -> },
        removed: (id: String, producer: Producer<*, *>) -> Unit = { _, _ -> }
    ) = mutate {
        producerListener(object : SenderOptions.ProducerListener {
            override fun producerAdded(id: String, producer: Producer<*, *>) {
                added(id, producer)
            }

            override fun producerRemoved(id: String, producer: Producer<*, *>) {
                removed(id, producer)
            }
        })
    }

    private fun mutate(f: SenderOptions<K, V>.() -> SenderOptions<K, V>) {
        underlying = f(underlying)
    }

    fun toSenderOptions(): SenderOptions<K, V> = underlying
}
