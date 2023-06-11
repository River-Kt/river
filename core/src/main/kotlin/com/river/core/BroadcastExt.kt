package com.river.core

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

fun <T> Flow<T>.broadcast(
    number: Int,
    buffer: Int = Channel.BUFFERED,
): Flow<List<Channel<T>>> =
    flow {
        val channels = (1..number).map { Channel<T>(buffer) }
        emit(channels)

        broadcast(channels)
            .launchCollect()
            .invokeOnCompletion { e ->
                channels.forEach { it.close(e) }
            }
    }

fun <T> Flow<T>.broadcast(
    vararg channels: Channel<T>
): Flow<T> = broadcast(channels.toList())

fun <T> Flow<T>.broadcast(
    channels: List<Channel<T>>
): Flow<T> =
    onEach { e -> channels.mapAsync { it.send(e) } }
