package io.github.gabfssilva.river.red.hat.debezium

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class CommittableOffset<R>(
    val numberOfRecords: Int,
    private val markProcessed: suspend (R) -> Unit,
    private val markBatchFinished: suspend () -> Unit
) {
    private var finished: Boolean = false
    private val committedRecords: MutableSet<R> = mutableSetOf()
    private val mutex = Mutex()

    suspend fun markProcessed(record: R) =
        mutex.withLock {
            if (!finished && committedRecords.add(record)) {
                markProcessed.invoke(record)

                if (committedRecords.size == numberOfRecords) {
                    finished = true
                    markBatchFinished()
                }
            }
        }
}
