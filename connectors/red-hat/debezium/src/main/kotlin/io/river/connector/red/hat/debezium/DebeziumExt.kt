package io.river.connector.red.hat.debezium

import io.debezium.engine.DebeziumEngine
import io.river.connector.red.hat.debezium.model.CommittableRecord
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import org.slf4j.LoggerFactory
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private val logger = LoggerFactory.getLogger("io.river.connector.red.hat.debezium.DebeziumFlow")

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
