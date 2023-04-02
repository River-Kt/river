package com.river.connector.red.hat.debezium.model

data class CommittableRecord<R>(
    val record: R,
    private val committer: CommittableOffset<R>
) {
    suspend fun markProcessed() =
        committer.markProcessed(record)
}
