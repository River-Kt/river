@file:OptIn(DelicateCoroutinesApi::class)

package com.river.core.internal


import com.river.core.internal.MutexBasedWindowedChunk.InternalChunk.State.*
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.*
import kotlin.time.Duration

internal class MutexBasedWindowedChunk<T>(
    private val sendChannel: SendChannel<List<T>>,
    private val window: Duration,
    private val size: Int
) {
    private val lock = Mutex()
    private var ref: InternalChunk<T> = newChunk()

    suspend fun append(item: T): Unit = lock.withLock {
        if (ref.isCompleted()) ref = newChunk()
        ref.append(item)
    }

    suspend fun complete(): Unit = lock.withLock { ref.complete() }

    private fun newChunk() = InternalChunk(window, size, lock, { sendChannel.send(it.items()) })

    class InternalChunk<T>(
        private val window: Duration,
        private val size: Int,
        private val lock: Mutex = Mutex(),
        private val onComplete: suspend (InternalChunk<T>) -> Unit,
        private val items: MutableList<T> = mutableListOf(),
        private var state: State = New
    ) {
        enum class State { New, OnGoing, Completed }

        private lateinit var timeout: Job

        fun items() = items.toList()
        fun isCompleted() = state == Completed

        suspend fun complete(onTimeout: Boolean = false) {
            if (!onTimeout && this::timeout.isInitialized) timeout.cancelAndJoin()

            if (state == OnGoing) {
                if (items.isNotEmpty()) onComplete(this)
                state = Completed
            }
        }

        suspend fun append(item: T) {
            if (state == Completed) error("Chunk is already completed")

            items.add(item)

            if (state == New) {
                state = OnGoing
                timeout = GlobalScope.launch { delay(window); lock.withLock { complete(true) } }
            }

            if (items.size == size) complete()
        }
    }
}
