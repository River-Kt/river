package com.river.core.internal

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import org.slf4j.LoggerFactory

internal class UnorderedMapParallelFlow<T, R>(
    private val upstream: Flow<T>,
    private val concurrencyLevel: Int,
    private val f: suspend (T) -> R
) : Flow<R> {
    private val logger by lazy { LoggerFactory.getLogger(this::class.java) }

    override suspend fun collect(collector: FlowCollector<R>): Unit =
        Semaphore(permits = concurrencyLevel)
            .let { semaphore ->
                val channel: Flow<R> =
                    channelFlow {
                        fun available() = semaphore.availablePermits

                        upstream
                            .collect {
                                semaphore.acquire()

                                logger.debug(
                                    "Running mapParallel. ${available()} " +
                                            "slots available of $concurrencyLevel"
                                )

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
