package io.river.util.http

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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

private val DefaultHttpClient: HttpClient = HttpClient.newHttpClient()

fun Flow<ByteBuffer>.asBodyPublisher(
    contentLength: Long = 0
): HttpRequest.BodyPublisher =
    if (contentLength < 1) fromPublisher(asPublisher())
    else fromPublisher(asPublisher(), contentLength)

suspend fun <T> HttpRequest.send(
    bodyHandler: BodyHandler<T>,
    client: HttpClient = DefaultHttpClient
): HttpResponse<T> =
    client
        .sendAsync(this, bodyHandler)
        .await()

fun method(
    name: String,
    url: String,
    f: RequestBuilder.() -> Unit = {}
) = RequestBuilder(url, name.uppercase()).also(f).build()

fun get(
    url: String,
    f: RequestBuilder.() -> Unit = {}
): HttpRequest = method("GET", url, f)

fun post(
    url: String,
    f: RequestBuilder.() -> Unit = {}
): HttpRequest = method("POST", url, f)

fun put(
    url: String,
    f: RequestBuilder.() -> Unit = {}
): HttpRequest = method("PUT", url, f)

fun delete(
    url: String,
    f: RequestBuilder.() -> Unit = {}
): HttpRequest = method("DELETE", url, f)

fun patch(
    url: String,
    f: RequestBuilder.() -> Unit = {}
): HttpRequest = method("PATCH", url, f)

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
