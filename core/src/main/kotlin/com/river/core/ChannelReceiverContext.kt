package com.river.core

import com.river.core.internal.InternalChannelReceiverContext

import kotlinx.coroutines.channels.Channel
import kotlin.time.Duration

/**
 * Simple wrapper around a channel that provides a context for receiving items in a more flexible manner.
 *
 * Example usage:
 *
 * ```
 * val channel = Channel<Int>()
 * val context = ChannelReceiverContext(channel)
 *
 * // Receive a single item
 * val item = context.next()
 *
 * // Receive multiple items
 * val items = context.next(5)
 *
 * // Receive multiple items with a timeout
 * val itemsWithTimeout = context.next(5, Duration.seconds(10))
 *
 * // Mark the context as completed
 * context.markAsCompleted()
 * ```
 */
interface ChannelReceiverContext<E> {
    /**
     * Awaits for the next item from the channel.
     *
     * @return The next item from the channel.
     */
    suspend fun next(): E

    /**
     * Awaits for the next N items from the channel. This method will suspend indefinitely until N items are received.
     *
     * @param n The number of items to receive from the channel.
     *
     * @return A list of the next N items from the channel.
     */
    suspend fun next(n: Int): List<E>

    /**
     * Awaits for the next N items from the channel, or until the timeout is reached. Once the timeout is reached,
     * the list of received items is returned, even if fewer than N items have been received.
     *
     * @param n The number of items to receive from the channel.
     * @param timeout The maximum time to wait for items.
     *
     * @return A list of the received items.
     */
    suspend fun next(n: Int, timeout: Duration): List<E>

    /**
     * Marks the receiver context as completed. After this method is called, no more items can be received from the channel.
     */
    fun markAsCompleted(): Unit

    companion object {
        /**
         * Creates a new ChannelReceiverContext for the given channel.
         *
         * @param channel The channel to create a context for.
         * @return A new ChannelReceiverContext for the given channel.
         */
        operator fun <E> invoke(channel: Channel<E>): ChannelReceiverContext<E> =
            InternalChannelReceiverContext(channel)
    }
}
