package com.river.core.internal

import com.river.core.launchCollect
import com.river.core.mapAsync
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

internal class Broadcast<T>(
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    buffer: Int = Channel.BUFFERED,
    upstream: Flow<T>,
    downstreamNumber: Int,
) {
    private val channels =
        (1..downstreamNumber)
            .map { Channel<T>(buffer) }

    private val flows: List<Flow<T>> =
        channels.map { it.receiveAsFlow() }

    init {
        upstream
            .onCompletion { channels.forEach { it.close() } }
            .launchCollect(scope) { element ->
                channels.mapAsync { it.send(element) }
            }
    }

    fun flows(): List<Flow<T>> = flows
}
