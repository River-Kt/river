package com.river.core.internal

import com.river.core.ThrottleStrategy
import com.river.core.tick
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
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
            val tuple by lazy {
                Semaphore(elementsPerInterval)
                    .let { s ->
                        s to tick(intervalDuration = interval) {
                            (1..(elementsPerInterval - s.availablePermits))
                                .forEach { _ -> s.release() }
                        }
                    }
            }

            fun semaphore() = tuple.first
            fun job() = tuple.second

            upstream
                .onCompletion { job().cancelAndJoin() }
                .collect {
                    when (strategy) {
                        ThrottleStrategy.Suspend -> {
                            semaphore().acquire()
                            emit(it)
                        }

                        ThrottleStrategy.Drop ->
                            if (semaphore().tryAcquire()) emit(it)
                    }
                }
        }
    }

    override suspend fun collect(collector: FlowCollector<T>) =
        inner.collect(collector)
}
