package com.river.connector.red.hat.debezium

import io.debezium.engine.DebeziumEngine
import com.river.connector.red.hat.debezium.model.CommittableRecord
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import org.slf4j.LoggerFactory
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private val logger = LoggerFactory.getLogger("com.river.connector.red.hat.debezium.DebeziumFlow")

/**
 * The debeziumFlow function creates a Kotlin Flow that consumes records from a Debezium engine
 * and emits them as CommittableRecords.
 *
 * Each CommittableRecord carries the original record and a CommittableOffset that allows the caller
 * to commit the record's offset to the Debezium engine once it has been successfully processed downstream.
 *
 * The capacity of the internal buffer that stores the unconsumed records is defined by [bufferCapacity].
 *
 * If the buffer fills up, the flow will start to suspend producers until more capacity becomes available again.
 *
 * In the other hand, [maxRecordsInFlight] represents the maximum number of records that can be outstanding at the same
 * time, waiting for their corresponding offsets to be committed. If this limit is reached, the flow will start to
 * suspend producers until more offsets are committed.
 *
 * Example usage:
 *
 * ```
 * val debeziumProperties: Properties = TODO("provide the debezium properties here")
 *
 * val flow = debeziumFlow { create(Json::class.java).using(debeziumProperties) }
 *
 * flow.collect { record ->
 *     println(record)
 *     record.markProcessed()
 * }
 * ```
 */
fun <R> debeziumFlow(
    bufferCapacity: Int = Channel.BUFFERED,
    maxRecordsInFlight: Int = 2048,
    executor: ExecutorService = Executors.newSingleThreadExecutor(),
    engineBuilder: () -> DebeziumEngine.Builder<R>
): Flow<CommittableRecord<R>> {
    logger.info("Initializing Debezium")

    val recordChannel = Channel<CommittableRecord<R>>(bufferCapacity)

    val engine: DebeziumEngine<R> =
        engineBuilder()
            .notifying(DebeziumChannelNotifier(maxRecordsInFlight, recordChannel))
            .using { success, message, error ->
                if (success) {
                    logger.info(message)
                } else {
                    logger.error(message ?: error.message, error)
                }

                if (success) recordChannel.close()
                else recordChannel.close(error)
            }
            .build()

    executor.execute(engine)

    logger.info("Debezium executor has started.")

    return recordChannel.consumeAsFlow().onCompletion {
        if (!(executor.isShutdown || executor.isTerminated)) {
            executor.shutdownNow()
        }
    }
}
