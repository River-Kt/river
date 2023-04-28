package com.river.core.internal

import com.river.core.GroupStrategy
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.RENDEZVOUS
import kotlinx.coroutines.flow.*

internal class SplitFlow<T>(
    private val upstream: Flow<T>,
    private val strategy: GroupStrategy
) : Flow<Flow<T>> {
    sealed interface Strategy<S : GroupStrategy> {
        companion object {
            fun <T> splitFlow(
                strategy: GroupStrategy,
                upstream: Flow<T>
            ) = when (strategy) {
                is GroupStrategy.Count ->
                    Count.splitFlow(strategy, upstream)

                is GroupStrategy.TimeWindow ->
                    TimeWindow.splitFlow(strategy, upstream)
            }
        }

        fun <T> splitFlow(
            strategy: S,
            upstream: Flow<T>
        ): Flow<Flow<T>>

        object Count : Strategy<GroupStrategy.Count> {
            override fun <T> splitFlow(
                strategy: GroupStrategy.Count,
                upstream: Flow<T>
            ): Flow<Flow<T>> =
                flow {
                    var emitted = 0
                    var chunk = Channel<T>(strategy.size)

                    val emitList: suspend () -> Unit = {
                        chunk.close()

                        if (emitted > 0) {
                            chunk = Channel(strategy.size)
                            emit(chunk.consumeAsFlow())
                        }

                        emitted = 0
                    }

                    upstream
                        .onCompletion {
                            chunk.close()
                        }
                        .collect {
                            if (emitted == 0) {
                                emit(chunk.consumeAsFlow())
                            }

                            if (emitted == strategy.size) {
                                emitList()
                            }

                            chunk.send(it)
                            emitted++
                        }
                }
        }

        object TimeWindow : Strategy<GroupStrategy.TimeWindow> {
            override fun <T> splitFlow(
                strategy: GroupStrategy.TimeWindow,
                upstream: Flow<T>
            ): Flow<Flow<T>> =
                channelFlow {
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

    override suspend fun collect(collector: FlowCollector<Flow<T>>): Unit =
        Strategy
            .splitFlow(strategy, upstream)
            .buffer(RENDEZVOUS)
            .collect(collector)
}
