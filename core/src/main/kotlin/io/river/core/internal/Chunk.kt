package io.river.core.internal

import io.river.core.ChunkStrategy
import kotlinx.coroutines.channels.Channel.Factory.RENDEZVOUS
import kotlinx.coroutines.flow.*

internal class Chunk<T>(
    private val upstream: Flow<T>,
    private val strategy: ChunkStrategy
) : Flow<List<T>> {
    sealed interface Strategy<S : ChunkStrategy> {
        companion object {
            fun <T> chunkedFlow(
                strategy: ChunkStrategy,
                upstream: Flow<T>
            ) = when (strategy) {
                is ChunkStrategy.Count ->
                    Count.chunkedFlow(strategy, upstream)

                is ChunkStrategy.TimeWindow ->
                    TimeWindow.chunkedFlow(strategy, upstream)
            }
        }

        fun <T> chunkedFlow(
            strategy: S,
            upstream: Flow<T>
        ): Flow<List<T>>

        object Count : Strategy<ChunkStrategy.Count> {
            override fun <T> chunkedFlow(
                strategy: ChunkStrategy.Count,
                upstream: Flow<T>
            ): Flow<List<T>> =
                flow {
                    val chunk = mutableListOf<T>()

                    val emitList: suspend () -> Unit = {
                        if (chunk.isNotEmpty()) {
                            val copy = chunk.toList()
                            chunk.clear()
                            emit(copy)
                        }
                    }

                    upstream
                        .onCompletion { emitList() }
                        .collect { chunk.add(it).also { if (chunk.size == strategy.size) emitList() } }
                }
        }

        object TimeWindow : Strategy<ChunkStrategy.TimeWindow> {
            override fun <T> chunkedFlow(
                strategy: ChunkStrategy.TimeWindow,
                upstream: Flow<T>
            ): Flow<List<T>> =
                channelFlow<List<T>> {
                    val windowedChunk =
                        MutexBasedWindowedChunk(
                            channel,
                            strategy.duration,
                            strategy.size
                        )

                    upstream
                        .onCompletion { windowedChunk.complete() }
                        .collect { windowedChunk.append(it) }
                }
        }
    }

    override suspend fun collect(collector: FlowCollector<List<T>>): Unit =
        Strategy
            .chunkedFlow(strategy, upstream)
            .buffer(RENDEZVOUS)
            .collect(collector)
}
