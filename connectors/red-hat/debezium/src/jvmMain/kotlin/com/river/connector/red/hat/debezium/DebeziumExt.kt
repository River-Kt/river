package com.river.connector.red.hat.debezium

import com.river.connector.red.hat.debezium.internal.DebeziumChannelNotifier
import com.river.connector.red.hat.debezium.model.CommittableOffset
import com.river.connector.red.hat.debezium.model.CommittableRecord
import com.river.core.ExperimentalRiverApi
import io.debezium.engine.DebeziumEngine
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.onCompletion
import org.slf4j.LoggerFactory
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private val logger = LoggerFactory.getLogger("com.river.connector.red.hat.debezium.DebeziumFlow")

/**
 * The debeziumFlow function creates a Kotlin Flow that consumes records from a Debezium engine
 * and emits them as CommittableRecords.
 *
 * Each [CommittableRecord] carries the original record and a [CommittableOffset] that allows the caller
 * to commit the record's offset to the Debezium engine once it has been successfully processed downstream.
 *
 * Once the [CommittableRecord] is processed, the `markProcessed` function must be called.
 *
 * The capacity of the internal buffer that stores the unconsumed records is defined by [maxRecordsInFlight].
 *
 * If the buffer fills up, the flow will start to suspend producers until more capacity becomes available again.
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
@ExperimentalRiverApi
fun <R> debeziumFlow(
    maxRecordsInFlight: Int = 250,
    executor: ExecutorService = Executors.newSingleThreadExecutor(),
    engineBuilder: () -> DebeziumEngine.Builder<R>
): Flow<CommittableRecord<R>> {
    logger.info("Initializing Debezium")

    val recordChannel = Channel<CommittableRecord<R>>(maxRecordsInFlight)

    val engine: DebeziumEngine<R> =
        engineBuilder()
            .notifying(DebeziumChannelNotifier(recordChannel))
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
