package com.river.connector.redis

import com.river.connector.redis.internal.RedisAsyncSemaphore
import com.river.core.AsyncSemaphore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi

import org.redisson.api.RedissonClient

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.future.await as coAwait

/**
 * Returns a suspending function that creates an [AsyncSemaphore] using a Redisson semaphore.
 * The semaphore is created the first time the returned function is invoked.
 *
 * @param name The name of the semaphore in Redisson.
 * @param concurrencyLevel The total number of permits this semaphore can provide.
 * @param leaseTime The time after which a permit is automatically released. Default value is 10 seconds.
 *
 * @return A suspending function that creates an [AsyncSemaphore] when invoked.
 */
@ExperimentalCoroutinesApi
fun RedissonClient.semaphore(
    name: String,
    concurrencyLevel: Int,
    leaseTime: Duration = 10.seconds
): suspend CoroutineScope.() -> AsyncSemaphore<String> = {
    val semaphore = getPermitExpirableSemaphore(name)
    semaphore.setPermitsAsync(concurrencyLevel).coAwait()
    RedisAsyncSemaphore(concurrencyLevel, this, leaseTime, semaphore)
}
