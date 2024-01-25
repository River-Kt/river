package com.river.core

import kotlinx.coroutines.flow.FlowCollector
import kotlin.coroutines.cancellation.CancellationException

interface StoppableFlowCollector<T> : FlowCollector<T> {
    suspend fun halt(msg: String = "cancelling flow"): Nothing =
        throw CancellationException(msg)

    companion object {
        operator fun <T> invoke(outer: FlowCollector<T>) =
            object : StoppableFlowCollector<T> {
                override suspend fun emit(value: T) {
                    outer.emit(value)
                }
            }
    }
}
