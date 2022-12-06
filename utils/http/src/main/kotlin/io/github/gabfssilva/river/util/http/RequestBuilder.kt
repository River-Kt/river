package io.github.gabfssilva.river.util.http

import io.github.gabfssilva.river.core.asByteArray
import io.github.gabfssilva.river.core.asByteBuffer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.jdk9.asPublisher
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublisher
import java.net.http.HttpRequest.BodyPublishers
import java.nio.ByteBuffer
import java.util.concurrent.Flow.Publisher

class RequestBuilder(
    val url: String,
    val method: String,
    var body: BodyPublisher = BodyPublishers.noBody(),
    val query: MutableMap<String, List<String>> = mutableMapOf(),
    val headers: MutableMap<String, List<String>> = mutableMapOf(),
    var expectContinue: Boolean = false
) {
    fun stringBody(body: String) = byteArrayBody(flowOf(body).asByteArray())

    fun byteArrayBody(body: Flow<ByteArray>, contentLength: Long? = null) =
        body(body.asByteBuffer(), contentLength)

    fun body(body: Flow<ByteBuffer>, contentLength: Long? = null) {
        body(body.asPublisher(), contentLength)
    }

    fun body(body: Publisher<ByteBuffer>, contentLength: Long? = null) {
        this.body = if (contentLength != null && contentLength > 0) {
            BodyPublishers.fromPublisher(body, contentLength)
        } else BodyPublishers.fromPublisher(body)
    }

    fun contentType(s: String) = header("Content-Type", s)

    private fun uri() =
        if (query.isEmpty()) URI(url)
        else {
            query
                .toList()
                .flatMap { (key, values) -> values.map { key to it } }
                .joinToString(separator = "&", prefix = "?") { (key, value) -> "$key=$value" }
                .let { URI("$url$it") }
        }

    fun build(): HttpRequest =
        HttpRequest
            .newBuilder(uri())
            .method(method, body)
            .also { builder ->
                headers.forEach { (key, values) -> values.forEach { value -> builder.setHeader(key, value) } }
            }
            .build()

    fun query(
        key: String,
        values: List<String>
    ) = query.put(key, values)

    fun query(
        key: String,
        vararg values: String?
    ) = values
        .mapNotNull { it }
        .let { if (it.isNotEmpty()) query(key, it) }

    fun query(
        vararg parameters: Pair<String, String?>,
    ) = parameters
        .map { (key, value) -> query(key, value) }

    fun header(
        key: String,
        values: List<String>
    ) = headers.put(key, values)

    fun header(
        key: String,
        vararg values: String
    ) = header(key, values.toList())
}
