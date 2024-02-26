package com.river.core.internal

import com.river.core.AsyncSemaphore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.map

internal class MapAsyncFlow<T, R, P>(
    private val upstream: Flow<T>,
    private val semaphore: suspend CoroutineScope.() -> AsyncSemaphore<P>,
    private val transform: suspend (T) -> R
) : Flow<R> {
    override suspend fun collect(collector: FlowCollector<R>) {
        coroutineScope {
            val semaphore = semaphore()

            upstream
                .map {
                    val id = semaphore.acquire()
                    async { id to transform(it) }
                }
                .buffer(semaphore.totalPermits)
                .map {
                    val (permit, result) = it.await()
                    semaphore.release(permit)
                    result
                }
                .collect(collector)

            semaphore.releaseAll()
        }
    }
}
