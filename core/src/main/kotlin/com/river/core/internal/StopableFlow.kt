package com.river.core.internal

import com.river.core.ExperimentalRiverApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.AbstractFlow
import kotlinx.coroutines.flow.FlowCollector

@ExperimentalCoroutinesApi
@ExperimentalRiverApi
internal class StoppableFlow<T>(
    private val block: suspend FlowCollector<T>.() -> Unit
) : AbstractFlow<T>() {
    override suspend fun collectSafely(collector: FlowCollector<T>) {
        runCatching { collector.block() }
            .getOrElse { it.also { if (it !is CancellationException) throw it } }
    }
}
