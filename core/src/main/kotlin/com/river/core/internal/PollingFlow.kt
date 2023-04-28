@file:OptIn(DelicateCoroutinesApi::class)

package com.river.core.internal

import com.river.core.ParallelismIncreaseStrategy
import com.river.core.ParallelismInfo
import com.river.core.mapParallel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import org.slf4j.LoggerFactory

internal class PollingFlow<T>(
    private val minimumParallelism: Int,
    private val maxParallelism: Int,
    private val stopOnEmptyList: Boolean = false,
    private val increaseStrategy: ParallelismIncreaseStrategy = ParallelismIncreaseStrategy.ByOne,
    private val producer: suspend ParallelismInfo.() -> List<T>
) : Flow<T> {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private val isCoroutineContextActive: Boolean
        get() = GlobalScope.coroutineContext[Job]?.isActive != false

    override suspend fun collect(collector: FlowCollector<T>) =
        flow {
            var gotEmptyResponse = false
            var firstIteration = true
            lateinit var lastParallelismInfo: ParallelismInfo

            fun shouldContinue(): Boolean =
                (firstIteration || !(stopOnEmptyList && gotEmptyResponse)) && isCoroutineContextActive

            while (shouldContinue()) {
                var emptyResultOnResponse = false

                lastParallelismInfo =
                    when {
                        firstIteration || gotEmptyResponse -> ParallelismInfo(maxParallelism, minimumParallelism)
                        else -> increaseStrategy.invoke(lastParallelismInfo)
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
