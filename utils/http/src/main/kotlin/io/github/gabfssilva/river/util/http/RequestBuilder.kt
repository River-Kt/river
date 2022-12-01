package io.github.gabfssilva.river.util.http

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import java.net.URI
import java.net.http.HttpRequest
import java.nio.ByteBuffer

class RequestBuilder(
    val url: String,
    val method: String,
    var body: () -> Pair<Long, Flow<ByteBuffer>> = { 0L to emptyFlow() },
    val query: MutableMap<String, List<String>> = mutableMapOf(),
    val headers: MutableMap<String, List<String>> = mutableMapOf(),
    var expectContinue: Boolean = false
) {
    fun byteArrayBody(contentLenght: Long = 0, f: () -> Flow<ByteArray>) = body(contentLenght) {
        f().map { ByteBuffer.wrap(it) }
    }

    fun body(contentLenght: Long = 0, f: () -> Flow<ByteBuffer>) {
        body = { contentLenght to f() }
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
            .method(method, body().let { (cl, flow) -> flow.asBodyPublisher(cl) })
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
