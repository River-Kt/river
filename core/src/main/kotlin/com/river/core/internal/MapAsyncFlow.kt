package com.river.core.internal

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Semaphore
import org.slf4j.LoggerFactory

internal class MapAsyncFlow<T, R>(
    private val upstream: Flow<T>,
    private val concurrency: Int,
    private val f: suspend (T) -> R
) : Flow<R> {
    private val logger by lazy { LoggerFactory.getLogger(this::class.java) }

    override suspend fun collect(collector: FlowCollector<R>): Unit =
        Semaphore(permits = concurrency)
            .let { semaphore ->
                val channel: Flow<Deferred<R>> =
                    flow {
                        coroutineScope {
                            fun available() = semaphore.availablePermits

                            upstream
                                .collect {
                                    semaphore.acquire()
                                    logger.debug("Running mapAsync. ${available()} " +
                                        "slots available of $concurrency")
                                    emit(async { f(it) })
                                }
                        }
                    }

                channel
                    .buffer(concurrency)
                    .map { it.await() }
                    .onEach { semaphore.release() }
            }
            .collect(collector)
}
