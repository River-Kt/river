package com.river.core.internal

import com.river.core.AsyncSemaphore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch

internal class UnorderedMapAsyncFlow<T, R, P>(
    private val upstream: Flow<T>,
    private val semaphore: suspend CoroutineScope.() -> AsyncSemaphore<P>,
    private val transform: suspend (T) -> R
) : Flow<R> {
    override suspend fun collect(collector: FlowCollector<R>) =
        channelFlow {
            val semaphore = semaphore()

            upstream
                .buffer(semaphore.totalPermits)
                .collect {
                    val id = semaphore.acquire()

                    launch {
                        send(transform(it))
                        semaphore.release(id)
                    }
                }

            semaphore.releaseAll()
        }.collect(collector)
}
