package io.github.gabfssilva.river.core.internal

import io.github.gabfssilva.river.core.collectAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.receiveAsFlow

internal class Broadcast<T>(
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    upstream: Flow<T>,
    downstreamNumber: Int,
) {
    private val channels = (1..downstreamNumber).map { Channel<T>() }
    private val flows: List<Flow<T>> = channels.map { it.receiveAsFlow() }

    init {
        upstream
            .onCompletion { channels.forEach { it.close() } }
            .collectAsync(scope) { element -> channels.forEach { it.send(element) } }
    }

    fun flows(): List<Flow<T>> = flows
}
