package com.river.connector.http

import com.river.core.splitEvery
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * [parseAsServerSentEvents] parses pieces of string in a stream fashion manner into [ServerSentEvent] objects.
 * It splits & chunks the flow using the "\n\n" delimiter, which separates different server-sent events in a stream.
 * Once the event is properly parsed, a [ServerSentEvent] is created and emitted downstream
 */
fun Flow<String>.parseAsServerSentEvents() =
    splitEvery("\n\n")
        .map(ServerSentEvent::parse)
