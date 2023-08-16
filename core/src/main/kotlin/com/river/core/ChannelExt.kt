package com.river.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch

@ExperimentalRiverApi
fun <E> CoroutineScope.consume(
    capacity: Int = Channel.RENDEZVOUS,
    onBufferOverflow: BufferOverflow = BufferOverflow.SUSPEND,
    onUndeliveredElement: ((E) -> Unit)? = null,
    consumer: suspend ReceiveChannel<E>.() -> Unit
): Channel<E> =
    Channel(capacity, onBufferOverflow, onUndeliveredElement)
        .also { launch { consumer(it) } }
