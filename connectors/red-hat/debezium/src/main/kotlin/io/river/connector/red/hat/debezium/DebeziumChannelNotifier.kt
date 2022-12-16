package io.river.connector.red.hat.debezium

import io.debezium.engine.DebeziumEngine
import io.river.connector.red.hat.debezium.model.CommittableOffset
import io.river.connector.red.hat.debezium.model.CommittableRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import org.slf4j.LoggerFactory

internal class DebeziumChannelNotifier<R>(
    maxRecordsInFlight: Int,
    private val recordChannel: SendChannel<CommittableRecord<R>>
) : DebeziumEngine.ChangeConsumer<R> {
    private val semaphore = Semaphore(maxRecordsInFlight)
    private val logger = LoggerFactory.getLogger(this.javaClass)

    override fun handleBatch(records: List<R>, committer: DebeziumEngine.RecordCommitter<R>) {
        runBlocking(Dispatchers.IO) {
            val batchSize = records.size

            logger.debug("Processing $batchSize items.")

            val committableOffset = CommittableOffset<R>(
                numberOfRecords = batchSize,
                markProcessed = {
                    logger.debug("Marking item as processed.")
                    committer.markProcessed(it)
                    semaphore.release()
                },
                markBatchFinished = {
                    logger.debug("Marking batch with $batchSize items as finished.")
                    committer.markBatchFinished()
                }
            )

            records
                .map { CommittableRecord(it, committableOffset) }
                .forEach {
                    semaphore.acquire()
                    recordChannel.send(it)
                }

            logger.debug("$batchSize items sent downstream.")
        }
    }
}
