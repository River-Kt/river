package com.river.connector.apache.kafka

import org.apache.kafka.common.TopicPartition
import reactor.core.scheduler.Scheduler
import reactor.kafka.receiver.ReceiverOptions
import reactor.kafka.receiver.ReceiverOptions.ConsumerListener
import reactor.kafka.receiver.ReceiverPartition
import java.util.function.Supplier
import kotlin.time.Duration
import kotlin.time.toJavaDuration
import org.apache.kafka.clients.consumer.Consumer as KafkaConsumer

class MutableReceiverOptions<K, V>(props: Map<String, Any>) {
    private var underlying: ReceiverOptions<K, V> = ReceiverOptions.create(props)

    sealed interface Subscription {
        companion object {
            fun topics(vararg topic: String): Subscription =
                Topics(topic.toList())

            fun pattern(pattern: java.util.regex.Pattern): Subscription =
                Pattern(pattern)

            fun partitionAssignment(vararg partition: TopicPartition): Subscription =
                PartitionAssignment(partition.toList())
        }
    }

    private class Topics(val topics: List<String>) : Subscription
    private class Pattern(val pattern: java.util.regex.Pattern) : Subscription
    private class PartitionAssignment(val partitions: List<TopicPartition>) : Subscription

    fun properties(builder: PropertyBuilder. () -> Unit) {
        PropertyBuilder()
            .also(builder)
            .properties()
            .forEach { (key, value) -> mutate { consumerProperty(key, value) } }
    }

    fun assignListener(onAssign: () -> Collection<ReceiverPartition>) =
        mutate { addAssignListener { onAssign() } }

    fun revokeListeners(onRevoke: () -> Collection<ReceiverPartition>) =
        mutate { addRevokeListener { onRevoke() } }

    fun keyDeserializer(transform: (topic: String, data: ByteArray) -> K) =
        mutate { withKeyDeserializer(transform) }

    fun scheduler(scheduler: () -> Scheduler) =
        mutate { schedulerSupplier(Supplier(scheduler)) }

    fun consumerListener(
        consumerAdded: (id: String, consumer: KafkaConsumer<*, *>) -> Unit = { _, _ -> },
        consumerRemoved: (id: String, consumer: KafkaConsumer<*, *>) -> Unit = { _, _ -> }
    ) = mutate {
        consumerListener(object : ConsumerListener {
            override fun consumerAdded(id: String, consumer: KafkaConsumer<*, *>) {
                consumerAdded(id, consumer)
            }

            override fun consumerRemoved(id: String, consumer: org.apache.kafka.clients.consumer.Consumer<*, *>) {
                consumerRemoved(id, consumer)
            }
        })
    }

    fun valueDeserializer(transform: (topic: String, data: ByteArray) -> V) {
        mutate { withValueDeserializer(transform) }
    }

    fun subscription(f: Subscription.Companion.() -> Subscription) = mutate {
        when (val subscription = f(Subscription)) {
            is Pattern -> subscription(subscription.pattern)
            is Topics -> subscription(subscription.topics)
            is PartitionAssignment -> assignment(subscription.partitions)
        }
    }

    fun pollTimeout(duration: Duration) = mutate { pollTimeout(duration.toJavaDuration()) }
    fun closeTimeout(duration: Duration) = mutate { closeTimeout(duration.toJavaDuration()) }
    fun commitInterval(duration: Duration) = mutate { commitInterval(duration.toJavaDuration()) }
    fun commitRetryInterval(duration: Duration) = mutate { commitRetryInterval(duration.toJavaDuration()) }
    fun maxDelayRebalance(duration: Duration) = mutate { maxDelayRebalance(duration.toJavaDuration()) }
    fun commitIntervalDuringDelay(interval: Long) = mutate { commitIntervalDuringDelay(interval) }
    fun commitBatchSize(size: Int) = mutate { commitBatchSize(size) }
    fun atmostOnceCommitAheadSize(size: Int) = mutate { atmostOnceCommitAheadSize(size) }
    fun maxCommitAttempts(size: Int) = mutate { maxCommitAttempts(size) }
    fun maxDeferredCommits(size: Int) = mutate { maxDeferredCommits(size) }

    private fun mutate(f: ReceiverOptions<K, V>.() -> ReceiverOptions<K, V>) {
        underlying = f(underlying)
    }

    fun toReceiverOptions() =
        underlying
}
