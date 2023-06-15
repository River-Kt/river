package com.river.core.internal

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Semaphore

internal class MapAsyncFlow<T, R>(
    private val upstream: Flow<T>,
    private val concurrency: Int,
    private val f: suspend (T) -> R
) : Flow<R> {
    override suspend fun collect(collector: FlowCollector<R>): Unit =
        Semaphore(permits = concurrency)
            .let { semaphore ->
                val channel: Flow<Deferred<R>> =
                    flow {
                        coroutineScope {
                            upstream
                                .collect {
                                    semaphore.acquire()
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
