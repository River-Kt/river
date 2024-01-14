package com.river.core.internal

import com.river.core.AsyncSemaphore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*

internal class MapAsyncFlow<T, R, P>(
    private val upstream: Flow<T>,
    private val semaphore: suspend CoroutineScope.() -> AsyncSemaphore<P>,
    private val transform: suspend (T) -> R
) : Flow<R> {
    override suspend fun collect(collector: FlowCollector<R>) {
        coroutineScope {
            val semaphore = semaphore()

            val channel: Flow<Deferred<Pair<P, R>>> =
                flow {
                    upstream
                        .collect {
                            val id = semaphore.acquire()
                            emit(async { id to transform(it) })
                        }
                }

            channel
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
