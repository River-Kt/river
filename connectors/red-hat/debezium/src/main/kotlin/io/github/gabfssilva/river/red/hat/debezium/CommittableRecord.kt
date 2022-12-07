package io.github.gabfssilva.river.red.hat.debezium

data class CommittableRecord<R>(
    val record: R,
    private val committer: CommittableOffset<R>
) {
    suspend fun markProcessed() =
        committer.markProcessed(record)
}
