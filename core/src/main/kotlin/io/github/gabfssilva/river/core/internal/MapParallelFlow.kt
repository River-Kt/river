package io.github.gabfssilva.river.core.internal

import io.github.gabfssilva.river.core.ConcurrencyInfo
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Semaphore
import org.slf4j.LoggerFactory

internal class MapParallelFlow<T, R>(
    private val upstream: Flow<T>,
    private val concurrencyLevel: Int,
    private val f: suspend ConcurrencyInfo.(T) -> R
) : Flow<R> {
    private val logger by lazy { LoggerFactory.getLogger(this::class.java) }

    override suspend fun collect(collector: FlowCollector<R>): Unit =
        Semaphore(permits = concurrencyLevel)
            .let { semaphore ->
                val channel: Flow<Deferred<R>> =
                    channelFlow {
                        val info = ConcurrencyInfo(concurrencyLevel, semaphore)

                        upstream
                            .collect {
                                semaphore.acquire()
                                logger.debug("Running mapParallel. ${info.availableSlots} " +
                                        "slots available of $concurrencyLevel")
                                send(async { f(info, it) })
                            }
                    }

                channel
                    .buffer(concurrencyLevel)
                    .map { it.await() }
                    .onEach { semaphore.release() }
            }
            .collect(collector)
}
