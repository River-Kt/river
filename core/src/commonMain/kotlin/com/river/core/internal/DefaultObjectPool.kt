package com.river.core.internal

import com.river.core.ObjectPool
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration

internal class DefaultObjectPool<T>(
    private val size: Int,
    private val maxDuration: Duration,
    initial: List<T>,
    val factory: suspend () -> T,
    val onClose: suspend (T) -> Unit
) : ObjectPool<T> {
    private val lock = Mutex()
    private var created = 0
    private val borrowed: MutableSet<ObjectPool.ObjectHolder<T>> = mutableSetOf()
    private val channel = Channel<ObjectPool.ObjectHolder<T>>(size)

    init {
        initial.forEach { channel.trySend(ObjectPool.ObjectHolder(it, maxDuration)) }
        created = initial.size
    }

    override suspend fun borrow(): ObjectPool.ObjectHolder<T> {
        val instance = channel.tryReceive().getOrNull()

        val obj = when {
            instance != null -> {
                instance
            }

            created >= size -> {
                channel.receive()
            }

            else -> lock.withLock {
                created++
                new()
            }
        }

        return lock.withLock {
            (if (obj.shouldBeClosed()) {
                onClose(obj.instance)
                created--
                new().also { created++ }
            } else obj).also { borrowed.add(it) }
        }
    }

    override suspend fun close(): Unit =
        lock.withLock {
            ((1..created).mapNotNull { channel.tryReceive().getOrNull() } + borrowed)
                .forEach {
                    onClose(it.instance)
                    created--
                }

            channel.close()
        }

    override suspend fun release(holder: ObjectPool.ObjectHolder<T>) {
        lock.withLock { borrowed.remove(holder) }
        channel.send(holder)
    }

    private suspend fun new(): ObjectPool.ObjectHolder<T> =
        ObjectPool.ObjectHolder(factory(), maxDuration)
}
