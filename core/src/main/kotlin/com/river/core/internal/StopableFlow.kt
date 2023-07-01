@file:OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)

package com.river.core.internal

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.AbstractFlow
import kotlinx.coroutines.flow.FlowCollector

internal class StoppableFlow<T>(
    private val block: suspend FlowCollector<T>.() -> Unit
) : AbstractFlow<T>() {
    override suspend fun collectSafely(collector: FlowCollector<T>) {
        runCatching { collector.block() }
            .getOrElse { it.also { if (it !is CancellationException) throw it } }
    }
}
