package com.river.core.internal

import com.river.core.ThrottleStrategy
import com.river.core.tick
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Semaphore
import kotlin.time.Duration

internal class ThrottleFlow<T>(
    elementsPerInterval: Int,
    interval: Duration,
    strategy: ThrottleStrategy = ThrottleStrategy.Suspend,
    upstream: Flow<T>
) : Flow<T> {

    private val inner = flow {
        coroutineScope {
            val (semaphore, job) =
                Semaphore(elementsPerInterval)
                    .let { s ->
                        s to tick(intervalDuration = interval) {
                            (1..(elementsPerInterval - s.availablePermits))
                                .forEach { _ -> s.release() }
                        }
                    }

            upstream
                .collect {
                    when (strategy) {
                        ThrottleStrategy.Suspend -> {
                            semaphore.acquire()
                            emit(it)
                        }

                        ThrottleStrategy.Drop ->
                            if (semaphore.tryAcquire()) emit(it)
                    }
                }

            job.cancelAndJoin()
        }
    }

    override suspend fun collect(collector: FlowCollector<T>) =
        inner.collect(collector)
}
