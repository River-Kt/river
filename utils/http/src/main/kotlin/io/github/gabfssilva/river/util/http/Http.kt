package io.github.gabfssilva.river.util.http

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.future.await
import kotlinx.coroutines.jdk9.asPublisher
import kotlinx.coroutines.jdk9.collect
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers.fromPublisher
import java.net.http.HttpResponse
import java.net.http.HttpResponse.*
import java.nio.ByteBuffer
import java.util.concurrent.CompletionStage
import java.util.concurrent.Flow.Publisher

private val DefaultHttpClient = HttpClient.newHttpClient()

fun Flow<ByteArray>.asBodyPublisher(): HttpRequest.BodyPublisher =
    fromPublisher(map { ByteBuffer.wrap(it) }.asPublisher())

suspend fun <T> HttpRequest.send(
    bodyHandler: BodyHandler<T>,
    client: HttpClient = DefaultHttpClient
): HttpResponse<T> =
    client
        .sendAsync(this, bodyHandler)
        .await()

fun get(
    url: String,
    f: RequestBuilder.() -> Unit = {}
): HttpRequest = RequestBuilder(url, "GET").also(f).build()

fun post(
    url: String,
    f: RequestBuilder.() -> Unit = {}
): HttpRequest = RequestBuilder(url, "POST").also(f).build()

fun put(
    url: String,
    f: RequestBuilder.() -> Unit = {}
): HttpRequest = RequestBuilder(url, "PUT").also(f).build()

fun delete(
    url: String,
    f: RequestBuilder.() -> Unit = {}
): HttpRequest = RequestBuilder(url, "DELETE").also(f).build()

fun patch(
    url: String,
    f: RequestBuilder.() -> Unit = {}
): HttpRequest = RequestBuilder(url, "PATCH").also(f).build()

fun <T> HttpResponse<Publisher<T>>.bodyAsFlow(): Flow<T> =
    flow { body().collect { emit(it) } }

fun BodyHandler<Publisher<List<ByteBuffer>>>.asFlow(): BodyHandler<Flow<ByteBuffer>> =
    BodyHandler {
        val subscriber = apply(it)

        object : BodySubscriber<Flow<ByteBuffer>> {
            override fun onSubscribe(subscription: java.util.concurrent.Flow.Subscription?) {
                subscriber.onSubscribe(subscription)
            }

            override fun onNext(item: MutableList<ByteBuffer>?) {
                subscriber.onNext(item)
            }

            override fun onError(throwable: Throwable?) {
                subscriber.onError(throwable)
            }

            override fun onComplete() {
                subscriber.onComplete()
            }

            override fun getBody(): CompletionStage<Flow<ByteBuffer>> =
                subscriber
                    .body
                    .thenApply { s -> flow { s.collect { it.forEach { emit(it) } } } }
        }
    }

val ofString = BodyHandlers.ofString()
val ofFlow = BodyHandlers.ofPublisher().asFlow()
val ofLines = BodyHandlers.ofLines()
val ofByteArray = BodyHandlers.ofByteArray()
