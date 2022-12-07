package io.github.gabfssilva.river.red.hat.debezium

import io.debezium.engine.DebeziumEngine
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

fun <R> debeziumFlow(
    bufferCapacity: Int = Channel.BUFFERED,
    maxRecordsInFlight: Int = 2048,
    executor: ExecutorService = Executors.newSingleThreadExecutor(),
    engineBuilder: () -> DebeziumEngine.Builder<R>
): Flow<CommittableRecord<R>> {
    val recordChannel = Channel<CommittableRecord<R>>(bufferCapacity)

    val engine: DebeziumEngine<R> =
        engineBuilder()
            .notifying(DebeziumChannelNotifier(maxRecordsInFlight, recordChannel))
            .using { success, _, error ->
                if (success) recordChannel.close()
                else recordChannel.close(error)

                if (!(executor.isShutdown || executor.isTerminated)) {
                    executor.shutdown()
                }
            }
            .build()

    executor.execute(engine)

    return recordChannel.consumeAsFlow()
}
