package com.river.connector.http

import java.net.http.HttpRequest

/**
 * Creates an HTTP request of the given method type to the specified URL.
 *
 * @param name The HTTP method name.
 * @param url The URL for the HTTP request.
 * @param f A lambda with receiver on `RequestBuilder` which allows to build the HTTP request.
 *
 * @return The created HTTP request.
 *
 * Example usage:
 *
 * ```
 * val request = method("POST", "https://example.com") {
 *     stringBody("Hello, world!")
 * }
 *
 * val response: HttpResponse<String> = request.coSend(ofString)
 * ```
 */
inline fun method(
    name: String,
    url: String,
    f: RequestBuilder.() -> Unit = {}
) = RequestBuilder(url, name.uppercase()).also(f).build()

/**
 * Creates an HTTP GET request to the specified URL.
 *
 * @param url The URL for the HTTP request.
 * @param f A lambda with receiver on `RequestBuilder` which allows to build the HTTP request.
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
    f: RequestBuilder.() -> Unit = {}
): HttpRequest = method("GET", url, f)

/**
 * Creates an HTTP POST request to the specified URL.
 *
 * @param url The URL for the HTTP request.
 * @param f A lambda with receiver on `RequestBuilder` which allows to build the HTTP request.
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
    f: RequestBuilder.() -> Unit = {}
): HttpRequest = method("POST", url, f)

/**
 * Creates an HTTP PUT request to the specified URL.
 *
 * @param url The URL for the HTTP request.
 * @param f A lambda with receiver on `RequestBuilder` which allows to build the HTTP request.
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
    f: RequestBuilder.() -> Unit = {}
): HttpRequest = method("PUT", url, f)

/**
 * Creates an HTTP DELETE request to the specified URL.
 *
 * @param url The URL for the HTTP request.
 * @param f A lambda with receiver on `RequestBuilder` which allows to build the HTTP request.
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
    f: RequestBuilder.() -> Unit = {}
): HttpRequest = method("DELETE", url, f)

/**
 * Creates an HTTP PATCH request to the specified URL.
 *
 * @param url The URL for the HTTP request.
 * @param f A lambda with receiver on `RequestBuilder` which allows to build the HTTP request.
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
    f: RequestBuilder.() -> Unit = {}
): HttpRequest = method("PATCH", url, f)
