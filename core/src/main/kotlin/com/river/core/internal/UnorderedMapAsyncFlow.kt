package com.river.core.internal

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore

internal class UnorderedMapAsyncFlow<T, R>(
    private val upstream: Flow<T>,
    private val concurrencyLevel: Int,
    private val f: suspend (T) -> R
) : Flow<R> {

    override suspend fun collect(collector: FlowCollector<R>): Unit =
        Semaphore(permits = concurrencyLevel)
            .let { semaphore ->
                val channel: Flow<R> =
                    channelFlow {
                        upstream
                            .collect {
                                semaphore.acquire()

                                launch {
                                    send(f(it))
                                    semaphore.release()
                                }
                            }
                    }

                channel.buffer(concurrencyLevel)
            }
            .collect(collector)
}
