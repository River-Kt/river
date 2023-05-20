package com.river.connector.http

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.jdk9.asPublisher
import java.net.URI
import java.net.http.HttpHeaders
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublisher
import java.net.http.HttpRequest.BodyPublishers.*
import java.net.http.HttpRequest.newBuilder
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.util.concurrent.Flow.Publisher

/**
 * A class that builds HTTP requests with custom features like query parameters and authorization.
 *
 * @property method An HTTP method to be used in the request.
 * @property other A builder instance of HttpRequest.Builder used to build the HTTP request.
 */
class CustomHttpRequestBuilder(
    private val method: HttpMethod,
    private val other: HttpRequest.Builder = newBuilder().method(method.name, noBody())
) : HttpRequest.Builder by other {
    private val queryParameters = mutableMapOf<String, List<String>>()

    /**
     * Adds multiple headers and overrides the old values
     *
     * @param headers The headers to be overridden.
     * @return Returns this builder instance.
     */
    fun setHeaders(
        headers: Map<String, List<String>>
    ): CustomHttpRequestBuilder {
        headers.forEach { (key, values) ->
            setHeader(key, values.firstOrNull())
            values.drop(1).forEach { header(key, it) }
        }

        return this
    }

    /**
     * Adds a query parameter with a single value.
     *
     * @param name The name of the query parameter.
     * @param value The value of the query parameter.
     * @return Returns this builder instance.
     */
    fun query(
        name: String,
        value: String
    ): CustomHttpRequestBuilder =
        query(name, listOf(value))

    /**
     * Adds a query parameter with multiple values.
     *
     * @param name The name of the query parameter.
     * @param values The values of the query parameter.
     * @return Returns this builder instance.
     */
    fun query(
        name: String,
        vararg values: String
    ): CustomHttpRequestBuilder =
        query(name, values.toList())

    /**
     * Adds a query parameter with a list of values.
     *
     * @param name The name of the query parameter.
     * @param values The values of the query parameter.
     * @return Returns this builder instance.
     */
    fun query(
        name: String,
        values: List<String>
    ): CustomHttpRequestBuilder {
        queryParameters[name] = values
        return this
    }

    /**
     * Sets the URI of the request.
     *
     * @param uri The URI to set.
     * @return Returns the HttpRequest.Builder instance.
     */
    override fun uri(uri: URI): HttpRequest.Builder =
        other.uri(
            when {
                queryParameters.isEmpty() -> uri
                else -> {
                    queryParameters
                        .toList()
                        .flatMap { (key, values) -> values.map { key to it } }
                        .joinToString(separator = "&", prefix = "?") { (key, value) -> "$key=$value" }
                        .let { URI("$uri$it") }
                }
            }
        )

    /**
     * Sets the Content-Type of the HTTP request.
     *
     * @param value The value of the Content-Type.
     * @return Returns the HttpRequest.Builder instance.
     */
    fun contentType(
        value: String
    ): HttpRequest.Builder = header("Content-Type", value)

    /**
     * Adds an authorization to the HTTP request.
     *
     * @param f A function to create an Authorization object.
     * @return Returns the HttpRequest.Builder instance.
     */
    inline fun authorization(
        f: Authorization.Companion.() -> Authorization
    ): HttpRequest.Builder = header(
        "Content-Type", f(Authorization.Companion).headerValue()
    )

    /**
     * Sets the body of the HTTP request as a String.
     *
     * @param value The value of the request body.
     * @param charset The charset to be used, default is Charset.defaultCharset().
     * @return Returns the HttpRequest.Builder instance.
     */
    fun stringBody(
        value: String,
        charset: Charset = Charset.defaultCharset()
    ): HttpRequest.Builder =
        body(ofString(value, charset))

    /**
     * Sets the body of the HTTP request as a ByteArray.
     *
     * @param value The value of the request body.
     * @return Returns the HttpRequest.Builder instance.
     */
    fun byteArrayBody(
        value: ByteArray
    ): HttpRequest.Builder =
        body(ofByteArray(value))

    /**
     * Sets the body of the HTTP request as a Flow<ByteBuffer>.
     *
     * @param value The value of the request body.
     * @param contentLength The content length of the request body.
     * @return Returns the HttpRequest.Builder instance.
     */
    fun flowBody(
        value: Flow<ByteBuffer>,
        contentLength: Long? = null
    ): HttpRequest.Builder =
        publisherBody(
            body = fromPublisher(value.asPublisher()),
            contentLength = contentLength
        )

    /**
     * Sets the body of the HTTP request as a Publisher<ByteBuffer>.
     *
     * @param body The body of the request.
     * @param contentLength The content length of the request body.
     * @return Returns the HttpRequest.Builder instance.
     */
    fun publisherBody(
        body: Publisher<ByteBuffer>,
        contentLength: Long? = null
    ): HttpRequest.Builder =
        when (contentLength) {
            null, 0L -> body(fromPublisher(body))
            else -> body(fromPublisher(body, contentLength))
        }

    /**
     * Sets the body of the HTTP request.
     *
     * @param body The BodyPublisher object.
     * @return Returns the HttpRequest.Builder instance.
     */
    fun body(
        body: BodyPublisher,
    ): HttpRequest.Builder =
        method(method.name, body)
}
