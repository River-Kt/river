@file:OptIn(DelicateCoroutinesApi::class)

package com.river.core.internal

import com.river.core.ParallelismInfo
import com.river.core.ParallelismStrategy
import com.river.core.mapParallel
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
        private val parallelism: ParallelismStrategy,
        private val stopOnEmptyList: Boolean = false,
        private val producer: suspend ParallelismInfo.() -> List<T>
    ) : PollingFlow<T> {
        private val logger = LoggerFactory.getLogger(this::class.java)

        override suspend fun collect(collector: FlowCollector<T>) =
            flow {
                var gotEmptyResponse = false
                var firstIteration = true
                lateinit var lastParallelismInfo: ParallelismInfo

                fun shouldContinue(): Boolean =
                    (firstIteration || !(stopOnEmptyList && gotEmptyResponse)) && isCoroutineContextActive()

                while (shouldContinue()) {
                    var emptyResultOnResponse = false

                    lastParallelismInfo =
                        when {
                            firstIteration || gotEmptyResponse -> parallelism.initial
                            else -> parallelism.increaseStrategy(lastParallelismInfo)
                        }

                    logger.debug(
                        "Polling using ${lastParallelismInfo.currentParallelism} " +
                            "of ${lastParallelismInfo.maxAllowedParallelism} total parallelism"
                    )

                    (1..lastParallelismInfo.currentParallelism)
                        .mapParallel { producer(lastParallelismInfo) }
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
            parallelism: ParallelismStrategy,
            stopOnEmptyList: Boolean,
            producer: suspend ParallelismInfo.() -> List<T>
        ): PollingFlow<T> {
            return when (parallelism.initial.maxAllowedParallelism) {
                1 -> Default(stopOnEmptyList) { producer(parallelism.initial) }

                else -> Parallel(
                    parallelism = parallelism,
                    stopOnEmptyList = stopOnEmptyList,
                    producer = producer
                )
            }
        }
    }
}
