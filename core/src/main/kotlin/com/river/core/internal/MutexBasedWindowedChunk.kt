@file:OptIn(DelicateCoroutinesApi::class)

package com.river.core.internal

import com.river.core.internal.MutexBasedWindowedChunk.InternalChunk.State.Completed
import com.river.core.internal.MutexBasedWindowedChunk.InternalChunk.State.New
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.sync.*
import kotlin.time.Duration

internal class MutexBasedWindowedChunk<T>(
    private val sendChannel: SendChannel<Flow<T>>,
    private val window: Duration,
    private val size: Int
) {
    private val lock = Mutex()
    private lateinit var ref: InternalChunk<T>

    suspend fun append(item: T): Unit = lock.withLock {
        if (!this::ref.isInitialized || ref.isCompleted()) {
            ref = newChunk()
            sendChannel.send(ref.asFlow)
        }

        ref.append(item)
    }

    suspend fun complete(): Unit =
        lock.withLock {
            if (this::ref.isInitialized) ref.complete()
        }

    private fun newChunk() =
        InternalChunk<T>(window, size, lock)

    class InternalChunk<T>(
        private val window: Duration,
        private val size: Int,
        private val lock: Mutex = Mutex(),
        private val items: Channel<T> = Channel(size),
        private var state: State = New
    ) {
        private var emittedItems = 0

        enum class State { New, OnGoing, Completed }

        private lateinit var timeout: Job

        fun isCompleted() = state == Completed

        val asFlow = items.consumeAsFlow()

        suspend fun complete(onTimeout: Boolean = false) {
            if (!onTimeout && this::timeout.isInitialized) timeout.cancelAndJoin()

            if (state == State.OnGoing) {
                items.close()
                state = Completed
            }
        }

        suspend fun append(item: T) {
            if (state == Completed) error("Chunk is already completed")

            items.send(item)
            emittedItems++

            if (state == New) {
                state = State.OnGoing
                timeout = GlobalScope.launch { delay(window); lock.withLock { complete(true) } }
            }

            if (emittedItems == size) complete()
        }
    }
}
