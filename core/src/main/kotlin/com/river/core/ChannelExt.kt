package com.river.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch

/**
 * Creates a [Channel] with the specified capacity, buffer overflow behavior, and undelivered element
 * handler, and then consumes elements from this channel using the given [consumer] function.
 *
 * As soon as the [Channel] or the [Job] finishes, one closes the other.
 *
 * @param capacity The capacity of the channel. Defaults to [Channel.RENDEZVOUS].
 * @param onBufferOverflow Specifies what to do when the buffer overflows. Defaults to [BufferOverflow.SUSPEND].
 * @param onUndeliveredElement Optional handler for undelivered elements. This is invoked when an element cannot be delivered to the consumer for any reason.
 * @param consumer A lambda with receiver of type [ReceiveChannel] which defines how to consume elements from the channel.
 *
 * @return The created [Channel] which the [consumer] is consuming from.
 *
 * Example usage:
 * ```
 * coroutineScope {
 *     val channel = consume<Int> {
 *         for (item in this) {
 *             println(item)
 *         }
 *     }
 *
 *     channel.send(1)
 *     channel.send(2)
 * }
 * ```
 */
@ExperimentalRiverApi
fun <E> CoroutineScope.consume(
    capacity: Int = Channel.RENDEZVOUS,
    onBufferOverflow: BufferOverflow = BufferOverflow.SUSPEND,
    onUndeliveredElement: ((E) -> Unit)? = null,
    consumer: suspend ReceiveChannel<E>.() -> Unit
): Channel<E> =
    Channel(capacity, onBufferOverflow, onUndeliveredElement)
        .also {
            launch {
                try {
                    consumer(it)
                } finally {
                    it.close()
                }
            }
        }
