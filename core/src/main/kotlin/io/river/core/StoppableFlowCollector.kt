package io.river.core

import io.river.core.internal.StopException
import kotlinx.coroutines.flow.FlowCollector

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