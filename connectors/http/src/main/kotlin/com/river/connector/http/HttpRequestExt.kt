package com.river.connector.http

import java.net.URI
import java.net.http.HttpRequest

/**
 * Creates an HTTP request of the given method type to the specified URL.
 *
 * @param uri The URI of the request.
 * @param method The [HttpMethod] of the request.
 * @param f A lambda with receiver on `CustomHttpRequestBuilder` which allows to build the HTTP request.
 *
 * @return The created HTTP request.
 *
 * Example usage:
 *
 * ```
 * val request = method("https://example.com", HttpMethod.Post) {
 *     stringBody("Hello, world!")
 * }
 *
 * val response: HttpResponse<String> = request.coSend(ofString)
 * ```
 */
inline fun request(
    uri: String,
    method: HttpMethod,
    f: CustomHttpRequestBuilder.() -> Unit = {}
): HttpRequest =
    CustomHttpRequestBuilder(method)
        .also(f)
        .uri(URI(uri))
        .build()

/**
 * Creates an HTTP GET request to the specified URL.
 *
 * @param url The URL for the HTTP request.
 * @param f A lambda with receiver on `CustomHttpRequestBuilder` which allows to build the HTTP request.
 *
 * @return The created HTTP GET request.
 *
 * Example usage:
 * ```
 * val request = get("https://example.com")
 *
 * val response: HttpResponse<String> = request.coSend(ofString)
 * ```
 */
inline fun get(
    url: String,
    f: CustomHttpRequestBuilder.() -> Unit = {}
) = request(url, HttpMethod.GET, f)


/**
 * Creates an HTTP POST request to the specified URL.
 *
 * @param url The URL for the HTTP request.
 * @param f A lambda with receiver on `CustomHttpRequestBuilder` which allows to build the HTTP request.
 *
 * @return The created HTTP POST request.
 *
 * Example usage:
 * ```
 * val request = post("https://example.com") {
 *     stringBody("Hello, world!")
 * }
 *
 * val response: HttpResponse<String> = request.coSend(ofString)
 * ```
 */
inline fun post(
    url: String,
    f: CustomHttpRequestBuilder.() -> Unit = {}
) = request(url, HttpMethod.POST, f)

/**
 * Creates an HTTP PUT request to the specified URL.
 *
 * @param url The URL for the HTTP request.
 * @param f A lambda with receiver on `CustomHttpRequestBuilder` which allows to build the HTTP request.
 *
 * @return The created HTTP PUT request.
 *
 * Example usage:
 * ```
 * val request = put("https://example.com") {
 *     stringBody("Hello, world!")
 * }
 *
 * val response: HttpResponse<String> = request.coSend(ofString)
 * ```
 */
inline fun put(
    url: String,
    f: CustomHttpRequestBuilder.() -> Unit = {}
) = request(url, HttpMethod.PUT, f)

/**
 * Creates an HTTP PATCH request to the specified URL.
 *
 * @param url The URL for the HTTP request.
 * @param f A lambda with receiver on `CustomHttpRequestBuilder` which allows to build the HTTP request.
 *
 * @return The created HTTP PATCH request.
 *
 * Example usage:
 * ```
 * val request = patch("https://example.com") {
 *     stringBody("Hello, world!")
 * }
 *
 * val response: HttpResponse<String> = request.coSend(ofString)
 * ```
 */
inline fun patch(
    url: String,
    f: CustomHttpRequestBuilder.() -> Unit = {}
) = request(url, HttpMethod.PATCH, f)

/**
 * Creates an HTTP OPTIONS request to the specified URL.
 *
 * @param url The URL for the HTTP request.
 * @param f A lambda with receiver on `CustomHttpRequestBuilder` which allows to build the HTTP request.
 *
 * @return The created HTTP OPTIONS request.
 *
 * Example usage:
 * ```
 * val request = options("https://example.com")
 * val response: HttpResponse<String> = request.coSend(ofString)
 * ```
 */
inline fun options(
    url: String,
    f: CustomHttpRequestBuilder.() -> Unit = {}
) = request(url, HttpMethod.OPTIONS, f)

/**
 * Creates an HTTP HEAD request to the specified URL.
 *
 * @param url The URL for the HTTP request.
 * @param f A lambda with receiver on `CustomHttpRequestBuilder` which allows to build the HTTP request.
 *
 * @return The created HTTP HEAD request.
 *
 * Example usage:
 * ```
 * val request = head("https://example.com")
 * val response: HttpResponse<String> = request.coSend(ofString)
 * ```
 */
inline fun head(
    url: String,
    f: CustomHttpRequestBuilder.() -> Unit = {}
) = request(url, HttpMethod.HEAD, f)

/**
 * Creates an HTTP DELETE request to the specified URL.
 *
 * @param url The URL for the HTTP request.
 * @param f A lambda with receiver on `CustomHttpRequestBuilder` which allows to build the HTTP request.
 *
 * @return The created HTTP DELETE request.
 *
 * Example usage:
 * ```
 * val request = delete("https://example.com")
 *
 * val response: HttpResponse<String> = request.coSend(ofString)
 * ```
 */
inline fun delete(
    url: String,
    f: CustomHttpRequestBuilder.() -> Unit = {}
) = request(url, HttpMethod.DELETE, f)
