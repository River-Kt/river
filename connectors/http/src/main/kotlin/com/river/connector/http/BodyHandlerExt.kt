package com.river.connector.http

import com.river.core.asByteArray
import com.river.core.asString
import com.river.core.flatten
import com.river.core.lines
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.jdk9.asFlow
import java.net.http.HttpResponse
import java.net.http.HttpResponse.BodyHandler
import java.net.http.HttpResponse.BodyHandlers
import java.net.http.HttpResponse.BodySubscribers.mapping
import java.net.http.HttpResponse.ResponseInfo
import java.nio.ByteBuffer

/**
 * This is a `BodyHandler` that handles HTTP response bodies as a String.
 */
val ofString: BodyHandler<String> = BodyHandlers.ofString()

/**
 * This is a `BodyHandler` that handles HTTP response bodies as a ByteArray.
 */
val ofByteArray: BodyHandler<ByteArray> = BodyHandlers.ofByteArray()

/**
 * This is a `BodyHandler` that discards the HTTP response body.
 */
val discarding: BodyHandler<Void> = BodyHandlers.discarding()

/**
 * This is a `BodyHandler` that handles HTTP response bodies as a Flow of ByteBuffer.
 */
val ofFlow: BodyHandler<Flow<ByteBuffer>> =
    BodyHandlers.ofPublisher().map { it.asFlow().flatten() }

/**
 * This is a `BodyHandler` that handles HTTP response bodies as a Flow of ByteArray.
 */
val ofByteArrayFlow: BodyHandler<Flow<ByteArray>> =
    ofFlow.map { it.asByteArray() }

/**
 * This is a `BodyHandler` that handles HTTP response bodies as a Flow of String.
 */
val ofStringFlow: BodyHandler<Flow<String>> =
    ofByteArrayFlow.map { it.asString() }

/**
 * This is a `BodyHandler` that handles HTTP response bodies as a Flow of Strings, where each string is a line from the response.
 */
val ofLines: BodyHandler<Flow<String>> =
    ofStringFlow.map { it.lines() }

/**
 * This is a `BodyHandler` that parses the HTTP response into [ServerSentEvent] objects.
 *
 * It uses [ofStringFlow] to convert the HTTP Response body into a flow of Strings.
 *
 */
val ofServerSentEventFlow: HttpResponse.BodyHandler<Flow<ServerSentEvent>> =
    ofStringFlow.map { it.parseAsServerSentEvents() }

/**
 * This function allows to create a new `BodyHandler` by transforming the output of the current `BodyHandler`.
 *
 * @param f The function to transform the output of the current `BodyHandler`.
 * @return A new `BodyHandler` that applies the transformation function to the output of the current `BodyHandler`.
 *
 * Example usage:
 *
 * ```
 * val ofInt: BodyHandler<Int> = ofString.map { it.toInt() }
 * ```
 */
fun <T, R> BodyHandler<T>.map(
    f: (T) -> R
): BodyHandler<R> = BodyHandler { mapping(apply(it)) { from -> f(from) } }
