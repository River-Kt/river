package com.river.connector.http

import com.river.core.splitEvery

import kotlinx.coroutines.flow.*

import java.net.http.HttpResponse

/**
 * This is a `BodyHandler` that parses the HTTP response into [ServerSentEvent] objects.
 *
 * It uses [ofStringFlow] to convert the HTTP Response body into a flow of Strings.
 * This [Flow] of Strings is then split on each occurrence of "\n\n", which separates different server-sent events in a stream.
 *
 * Once the event is properly parsed, a [ServerSentEvent] is created and emitted downstream
 */
val ofServerSentEventFlow: HttpResponse.BodyHandler<Flow<ServerSentEvent>> =
    ofStringFlow
        .map { upstream ->
            flow {
                upstream
                    .splitEvery("\n\n")
                    .collect { eventLines ->
                        var id: String? = null
                        var event: String? = null
                        val data = mutableListOf<String>()
                        val comments = mutableListOf<String>()

                        val lines = eventLines.split("\n")

                        for (line in lines) {
                            when {
                                line.startsWith("id:") ->
                                    id = line.removePrefix("id: ").trim()
                                line.startsWith("event:") ->
                                    event = line.removePrefix("event: ").trim()
                                line.startsWith("data:") ->
                                    data.add(line.removePrefix("data: ").trim())
                                line.startsWith(":") ->
                                    comments.add(line.removePrefix(":").trim())
                            }
                        }

                        emit(ServerSentEvent(id, event, data, comments))
                    }
            }
        }
