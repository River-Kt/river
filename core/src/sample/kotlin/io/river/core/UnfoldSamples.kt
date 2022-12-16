package io.river.core

import kotlinx.coroutines.flow.Flow

class UnfoldSamples {
    suspend fun infiniteIntFlow() {
        val counter = java.util.concurrent.atomic.AtomicInteger()

        val flow: Flow<Int> = unfold { listOf(counter.incrementAndGet()) }

        flow.collect(::println) //1, 2, 3, 4, 5, ..., ∞
    }

    suspend fun queuePollingFlow(
        queue: kotlinx.coroutines.channels.ReceiveChannel<List<String>>
    ) {
        val queueAsFlow: Flow<String> = unfold(stopOnEmptyList = true) { queue.receive() }

        queueAsFlow.collect(::println)
    }
}
