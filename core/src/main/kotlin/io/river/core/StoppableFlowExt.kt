package io.river.core

import io.river.core.internal.StopException
import io.river.core.internal.StoppableFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector

fun <T> stoppableFlow(block: suspend StoppableFlowCollector<T>.() -> Unit): Flow<T> =
    StoppableFlow { block(StoppableFlowCollector(this)) }

interface StoppableFlowCollector<T> : FlowCollector<T> {
    suspend fun halt(msg: String = "cancelling flow"): Nothing = throw StopException(msg)

    companion object {
        operator fun <T> invoke(outer: FlowCollector<T>) =
            object : StoppableFlowCollector<T> {
                override suspend fun emit(value: T) {
                    outer.emit(value)
                }
            }
    }
}