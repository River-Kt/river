package com.river.core.internal

import com.river.core.AsyncSemaphore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch

internal class UnorderedMapAsyncFlow<T, R>(
    private val upstream: Flow<T>,
    private val semaphore: suspend CoroutineScope.() -> AsyncSemaphore,
    private val transform: suspend (T) -> R
) : Flow<R> {
    override suspend fun collect(collector: FlowCollector<R>) {
        coroutineScope {
            val semaphore = semaphore()

            val channel: Flow<R> =
                channelFlow {
                    upstream
                        .collect {
                            val id = semaphore.acquire()

                            launch {
                                send(transform(it))
                                semaphore.release(id)
                            }
                        }
                }

            channel
                .buffer(semaphore.totalPermits)
                .collect(collector)

            semaphore.releaseAll()
        }
    }
}
