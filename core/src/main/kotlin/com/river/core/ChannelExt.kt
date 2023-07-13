package com.river.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch

fun <E> CoroutineScope.launchChannelConsumer(
    capacity: Int = Channel.RENDEZVOUS,
    onBufferOverflow: BufferOverflow = BufferOverflow.SUSPEND,
    onUndeliveredElement: ((E) -> Unit)? = null,
    consumer: suspend ReceiveChannel<E>.() -> Unit
): Channel<E> =
    Channel(capacity, onBufferOverflow, onUndeliveredElement)
        .also { launch { consumer(it) } }
