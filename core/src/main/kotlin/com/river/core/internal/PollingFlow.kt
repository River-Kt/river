@file:OptIn(DelicateCoroutinesApi::class)

package com.river.core.internal

import com.river.core.ConcurrencyInfo
import com.river.core.ConcurrencyStrategy
import com.river.core.mapAsync
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import org.slf4j.LoggerFactory

internal sealed interface PollingFlow<T> : Flow<T> {
    class Default<T>(
        private val stopOnEmptyList: Boolean = false,
        private val producer: suspend () -> List<T>
    ) : PollingFlow<T> {
        override suspend fun collect(collector: FlowCollector<T>) =
            flow {
                while (isCoroutineContextActive()) {
                    val result = producer()

                    result.forEach { emit(it) }

                    if (result.isEmpty() && stopOnEmptyList) {
                        break;
                    }
                }
            }.collect(collector)
    }

    class Parallel<T>(
        private val concurrency: ConcurrencyStrategy,
        private val stopOnEmptyList: Boolean = false,
        private val producer: suspend ConcurrencyInfo.() -> List<T>
    ) : PollingFlow<T> {
        private val logger = LoggerFactory.getLogger(this::class.java)

        override suspend fun collect(collector: FlowCollector<T>) =
            flow {
                var gotEmptyResponse = false
                var firstIteration = true
                lateinit var lastConcurrencyInfo: ConcurrencyInfo

                fun shouldContinue(): Boolean =
                    (firstIteration || !(stopOnEmptyList && gotEmptyResponse)) && isCoroutineContextActive()

                while (shouldContinue()) {
                    var emptyResultOnResponse = false

                    lastConcurrencyInfo =
                        when {
                            firstIteration || gotEmptyResponse -> concurrency.initial
                            else -> concurrency.increaseStrategy(lastConcurrencyInfo)
                        }

                    logger.debug(
                        "Polling using ${lastConcurrencyInfo.current} " +
                            "of ${lastConcurrencyInfo.maximum} total concurrency"
                    )

                    (1..lastConcurrencyInfo.current)
                        .mapAsync { producer(lastConcurrencyInfo) }
                        .onEach { if (!emptyResultOnResponse) emptyResultOnResponse = it.isEmpty() }
                        .flatten()
                        .also {
                            logger.debug("Done polling.")

                            if (it.isNotEmpty()) {
                                logger.debug("Emitting ${it.size} items downstream")
                            } else logger.debug("No items returned.")
                        }
                        .forEach {
                            emit(it)
                        }

                    firstIteration = false
                    gotEmptyResponse = emptyResultOnResponse
                }

                logger.debug("Stopping poll...")
            }.collect(collector)
    }

    fun isCoroutineContextActive(): Boolean = GlobalScope.coroutineContext[Job]?.isActive != false

    companion object {
        operator fun <T> invoke(
            concurrency: ConcurrencyStrategy,
            stopOnEmptyList: Boolean,
            producer: suspend ConcurrencyInfo.() -> List<T>
        ): PollingFlow<T> {
            return when (concurrency.initial.maximum) {
                1 -> Default(stopOnEmptyList) { producer(concurrency.initial) }

                else -> Parallel(
                    concurrency = concurrency,
                    stopOnEmptyList = stopOnEmptyList,
                    producer = producer
                )
            }
        }
    }
}
