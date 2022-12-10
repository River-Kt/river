@file:OptIn(FlowPreview::class)

package io.river.core.internal

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.AbstractFlow
import kotlinx.coroutines.flow.FlowCollector

internal class StopException(msg: String) : RuntimeException(msg)

internal class StoppableFlow<T>(
    private val block: suspend FlowCollector<T>.() -> Unit
) : AbstractFlow<T>() {
    override suspend fun collectSafely(collector: FlowCollector<T>) {
        runCatching { collector.block() }
            .getOrElse { it.also { if (it !is StopException) throw it } }
    }
}
