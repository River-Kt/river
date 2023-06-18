package com.river.connector.red.hat.debezium

import com.river.connector.red.hat.debezium.model.CommittableOffset
import com.river.connector.red.hat.debezium.model.CommittableRecord
import io.debezium.engine.DebeziumEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

internal class DebeziumChannelNotifier<R>(
    private val recordChannel: SendChannel<CommittableRecord<R>>
) : DebeziumEngine.ChangeConsumer<R> {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    override fun handleBatch(records: List<R>, committer: DebeziumEngine.RecordCommitter<R>) {
        runBlocking(Dispatchers.IO) {
            val batchSize = records.size

            logger.debug("Processing $batchSize items.")

            val committableOffset = CommittableOffset<R>(
                numberOfRecords = batchSize,
                markProcessed = {
                    withContext(Dispatchers.IO) {
                        logger.debug("Marking item as processed.")
                        committer.markProcessed(it)
                    }
                },
                markBatchFinished = {
                    withContext(Dispatchers.IO) {
                        logger.debug("Marking batch with $batchSize items as finished.")
                        committer.markBatchFinished()
                    }
                }
            )

            records
                .map { CommittableRecord(it, committableOffset) }
                .forEach { recordChannel.send(it) }

            logger.debug("$batchSize items sent downstream.")
        }
    }
}
