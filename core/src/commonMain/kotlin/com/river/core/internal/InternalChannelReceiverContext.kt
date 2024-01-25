package com.river.core.internal

import com.river.core.ChannelReceiverContext
import com.river.core.ExperimentalRiverApi
import com.river.core.toList
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flow
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration

@ExperimentalRiverApi
internal class InternalChannelReceiverContext<T>(
    private val channel: Channel<T>
) : ChannelReceiverContext<T> {
    private val receiverFlow = flow {
        while (!isCompleted()) {
            emit(channel.receive())
        }
    }

    private val completionSignal: CompletableDeferred<Unit> = CompletableDeferred()
    private fun isCompleted() = completionSignal.isCompleted

    private fun ensureNotCompleted() {
        if (isCompleted()) {
            throw CancellationException("channel receiver is completed")
        }
    }

    private fun receiverFlowIfNotCompleted() =
        ensureNotCompleted().let { receiverFlow }

    override fun markAsCompleted() {
        completionSignal.complete(Unit)
    }

    override suspend fun next() =
        ensureNotCompleted().let { channel.receive() }

    override suspend fun next(n: Int): List<T> =
        receiverFlowIfNotCompleted()
            .toList(n)

    override suspend fun next(n: Int, timeout: Duration): List<T> =
        receiverFlowIfNotCompleted()
            .toList(n, timeout)
}
