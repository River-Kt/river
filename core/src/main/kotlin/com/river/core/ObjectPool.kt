package com.river.core

import com.river.core.internal.DefaultObjectPool
import java.time.ZonedDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

interface ObjectPool<T> {
    class ObjectHolder<T>(
        val instance: T,
        val maxDuration: Duration,
        val createdAt: ZonedDateTime
    ) {
        fun shouldBeClosed(): Boolean =
            ZonedDateTime.now() >= createdAt.plus(maxDuration.toJavaDuration())
    }

    suspend fun borrow(): ObjectHolder<T>
    suspend fun release(holder: ObjectHolder<T>)
    suspend fun close()

    suspend fun <R> borrow(f: suspend (T) -> R): R {
        val obj = borrow()
        return f(obj.instance).also { release(obj) }
    }
}

fun <T> objectPool(
    maxSize: Int,
    maxDuration: Duration = 5.minutes,
    onClose: suspend (T) -> Unit = {},
    factory: suspend () -> T,
): ObjectPool<T> = DefaultObjectPool(maxSize, maxDuration, emptyList(), factory, onClose)

suspend fun <T> objectPool(
    maxSize: Int,
    initialSize: Int = 1,
    maxDuration: Duration = 5.minutes,
    onClose: suspend (T) -> Unit = {},
    factory: suspend () -> T,
): ObjectPool<T> =
    DefaultObjectPool(
        size = maxSize,
        maxDuration = maxDuration,
        initial = (1..initialSize).map { factory() },
        factory = factory,
        onClose = onClose
    )
