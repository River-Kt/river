package io.github.gabfssilva.river.util.http

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import java.net.URI
import java.net.http.HttpRequest

class RequestBuilder(
    private val url: String,
    private val method: String,
    private var body: () -> Flow<ByteArray> = { emptyFlow() },
    private val headers: MutableMap<String, List<String>> = mutableMapOf()
) {
    fun body(f: () -> Flow<ByteArray>) {
        body = f
    }

    fun contentType(s: String) = header("Content-Type", s)

    fun build(): HttpRequest =
        HttpRequest
            .newBuilder(URI(url))
            .method(method, body().asBodyPublisher())
            .also { builder ->
                headers.forEach { (key, values) -> values.forEach { value -> builder.setHeader(key, value) } }
            }
            .build()

    fun header(key: String, values: List<String>) = headers.put(key, values)
    fun header(key: String, vararg values: String) = headers.put(key, values.toList())
}
