package io.github.gabfssilva.river.util.pool

import java.time.ZonedDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

interface ObjectPool<T> {
    suspend fun borrow(): ObjectHolder<T>
    suspend fun release(holder: ObjectHolder<T>)
    suspend fun close()

    class ObjectHolder<T>(
        val instance: T,
        val maxDuration: Duration,
        val createdAt: ZonedDateTime
    ) {
        fun shouldBeClosed(): Boolean =
            ZonedDateTime.now() >= createdAt.plus(maxDuration.toJavaDuration())
    }

    suspend fun <R> use(f: suspend (T) -> R): R {
        val obj = borrow()
        return f(obj.instance).also { release(obj) }
    }

    companion object {
        fun <T> sized(
            maxSize: Int,
            maxDuration: Duration = 5.minutes,
            onClose: suspend (T) -> Unit = {},
            factory: suspend () -> T,
        ): ObjectPool<T> = DefaultObjectPool(maxSize, maxDuration, emptyList(), factory, onClose)

        suspend fun <T> sized(
            maxSize: Int,
            initialSize: Int = 1,
            maxDuration: Duration = 5.minutes,
            onClose: suspend (T) -> Unit = {},
            factory: suspend () -> T,
        ): ObjectPool<T> =
            DefaultObjectPool(maxSize, maxDuration, (1..initialSize).map { factory() }, factory, onClose)
    }
}
