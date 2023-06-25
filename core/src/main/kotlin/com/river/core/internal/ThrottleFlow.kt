package com.river.core.internal

import com.river.core.AsyncSemaphore
import com.river.core.ThrottleStrategy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow

internal class ThrottleFlow<T>(
    semaphore: suspend CoroutineScope.() -> AsyncSemaphore,
    strategy: ThrottleStrategy = ThrottleStrategy.Suspend,
    upstream: Flow<T>
) : Flow<T> {
    private val inner = flow {
        coroutineScope {
            val asyncSemaphore = semaphore()

            upstream
                .collect {
                    when (strategy) {
                        ThrottleStrategy.Suspend -> {
                            asyncSemaphore.acquire()
                            emit(it)
                        }

                        ThrottleStrategy.Drop ->
                            if (asyncSemaphore.tryAcquire() != null) emit(it)
                    }
                }
        }
    }

    override suspend fun collect(collector: FlowCollector<T>) =
        inner.collect(collector)
}
