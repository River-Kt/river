package com.river.connector.aws.ses

import com.river.core.mapAsync
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.future.await
import software.amazon.awssdk.services.ses.SesAsyncClient
import software.amazon.awssdk.services.ses.model.*

/**
 * Transforms the given flow of items into a flow of [SendEmailRequest]s,
 * each built by applying the specified builder function to each item.
 *
 * @param f The function to apply to each item to build the [SendEmailRequest].
 * @return A new flow of [SendEmailRequest]s.
 *
 * Example usage:
 * ```kotlin
 * val flow = flowOf("Hello", "World")
 * val emailFlow = flow.sendEmailRequest { value ->
 *     destination { it.toAddresses("example@example.com") }
 *     subject("Test Subject")
 *     body(value)
 * }
 * ```
 */
fun <T> Flow<T>.sendEmailRequest(
    f: SendEmailRequest.Builder.(T) -> Unit
): Flow<SendEmailRequest> =
    map { SendEmailRequest.builder().also { b -> f(b, it) }.build() }

/**
 * Transforms the given flow of items into a flow of [SendTemplatedEmailRequest]s,
 * each built by applying the specified builder function to each item.
 *
 * @param f The function to apply to each item to build the [SendTemplatedEmailRequest].
 * @return A new flow of [SendTemplatedEmailRequest]s.
 *
 * Example usage:
 * ```kotlin
 * val flow = flowOf("Hello", "World")
 * val emailFlow = flow.sendTemplatedEmailRequest { value ->
 *     destination { it.toAddresses("example@example.com") }
 *     template("Test Template")
 *     templateData(value)
 * }
 * ```
 */
fun <T> Flow<T>.sendTemplatedEmailRequest(
    f: SendTemplatedEmailRequest.Builder.(T) -> Unit
): Flow<SendTemplatedEmailRequest> =
    map { SendTemplatedEmailRequest.builder().also { b -> f(b, it) }.build() }

/**
 * Transforms the given flow of items into a flow of [SendBulkTemplatedEmailRequest]s,
 * each built by applying the specified builder function to each item.
 *
 * @param f The function to apply to each item to build the [SendBulkTemplatedEmailRequest].
 * @return A new flow of [SendBulkTemplatedEmailRequest]s.
 *
 * Example usage:
 * ```kotlin
 * val flow = flowOf("Hello", "World")
 * val emailFlow = flow.sendBulkTemplatedEmailRequest { value ->
 *     destinations({
 *         it.toAddresses("example@example.com")
 *     }, {
 *         it.toAddresses("example@example.com")
 *     })
 *
 *     template("Test Template")
 *     templateData(value)
 * }
 * ```
 */
fun <T> Flow<T>.sendBulkTemplatedEmailRequest(
    f: SendBulkTemplatedEmailRequest.Builder.(T) -> Unit
): Flow<SendBulkTemplatedEmailRequest> =
    map { SendBulkTemplatedEmailRequest.builder().also { b -> f(b, it) }.build() }

/**
 * Sends an email for each item in the given upstream flow, using this client to send the emails.
 * The specified function is applied to each item to build the [SendEmailRequest] to send.
 * Requests are sent concurrently, up to the specified maximum concurrency.
 *
 * @param upstream The flow of items to send emails for.
 * @param concurrency The maximum number of concurrent requests. Default is 1.
 * @param f The function to apply to each item to build the [SendEmailRequest].
 * @return A new flow of [SendEmailResponse]s.
 *
 * Example usage:
 * ```kotlin
 * val client = SesAsyncClient.create()
 * val flow = flowOf("Hello", "World")
 * val responseFlow = client.sendEmailFlow(flow, 2) { value ->
 *     destination { toAddresses("example@example.com") }
 *     subject("Test Subject")
 *     body(value)
 * }
 * ```
 */
fun <T> SesAsyncClient.sendEmailFlow(
    upstream: Flow<T>,
    concurrency: Int = 1,
    f: SendEmailRequest.Builder.(T) -> Unit
): Flow<SendEmailResponse> =
    sendEmailFlow(upstream.sendEmailRequest(f), concurrency)

/**
 * Sends an email for each [SendEmailRequest] in the given upstream flow, using this client to send the emails.
 *
 * @param upstream The flow of [SendEmailRequest]s to send.
 * @param concurrency The maximum number of concurrent requests. Default is 1.
 * @return A new flow of [SendEmailResponse]s.
 *
 * Example usage:
 * ```kotlin
 * val client = SesAsyncClient.create()
 * val requests = flowOf(SendEmailRequest.builder().destination { toAddresses("example@example.com") }.subject("Test Subject").body("Hello").build())
 * val responseFlow = client.sendEmailFlow(requests, 2)
 * ```
 */
fun SesAsyncClient.sendEmailFlow(
    upstream: Flow<SendEmailRequest>,
    concurrency: Int = 1
): Flow<SendEmailResponse> =
    upstream.mapAsync(concurrency) { sendEmail(it).await() }

/**
 * Sends a templated email for each item in the given upstream flow, using this client to send the emails.
 * The specified function is applied to each item to build the [SendTemplatedEmailRequest] to send.
 * Requests are sent concurrently, up to the specified maximum concurrency.
 *
 * @param upstream The flow of items to send emails for.
 * @param concurrency The maximum number of concurrent requests. Default is 1.
 * @param f The function to apply to each item to build the [SendTemplatedEmailRequest].
 * @return A new flow of [SendTemplatedEmailResponse]s.
 *
 * Example usage:
 * ```kotlin
 * val client = SesAsyncClient.create()
 * val flow = flowOf("Hello", "World")
 * val responseFlow = client.sendTemplatedEmailFlow(flow, 2) { value ->
 *     destination { it.toAddresses("example@example.com") }
 *     template("Test Template")
 *     templateData(value)
 * }
 * ```
 */
fun <T> SesAsyncClient.sendTemplatedEmailFlow(
    upstream: Flow<T>,
    concurrency: Int = 1,
    f: SendTemplatedEmailRequest.Builder.(T) -> Unit
): Flow<SendTemplatedEmailResponse> =
    sendTemplatedEmailFlow(upstream.sendTemplatedEmailRequest(f), concurrency)

/**
 * Sends a templated email for each [SendTemplatedEmailRequest] in the given upstream flow, using this client to send the emails.
 *
 * @param upstream The flow of [SendTemplatedEmailRequest]s to send.
 * @param concurrency The maximum number of concurrent requests. Default is 1.
 * @return A new flow of [SendTemplatedEmailResponse]s.
 *
 * Example usage:
 * ```kotlin
 * val client = SesAsyncClient.create()
 * val requests = flowOf(SendTemplatedEmailRequest.builder().destination { toAddresses("example@example.com") }.template("Test Template").templateData("Hello").build())
 * val responseFlow = client.sendTemplatedEmailFlow(requests, * 2)
 *```
 */
fun SesAsyncClient.sendTemplatedEmailFlow(
    upstream: Flow<SendTemplatedEmailRequest>,
    concurrency: Int = 1
): Flow<SendTemplatedEmailResponse> =
    upstream.mapAsync(concurrency) { sendTemplatedEmail(it).await() }

/**
 * Sends a bulk templated email for each item in the given upstream flow, using this client to send the emails.
 * The specified function is applied to each item to build the [SendBulkTemplatedEmailRequest] to send.
 * Requests are sent concurrently, up to the specified maximum concurrency.
 *
 * @param upstream The flow of items to send emails for.
 * @param concurrency The maximum number of concurrent requests. Default is 1.
 * @param f The function to apply to each item to build the [SendBulkTemplatedEmailRequest].
 * @return A new flow of [SendBulkTemplatedEmailResponse]s.
 *
 * Example usage:
 * ```kotlin
 * val client = SesAsyncClient.create()
 * val flow = flowOf("Hello", "World")
 * val responseFlow = client.sendBulkTemplatedEmailFlow(flow, 2) { value ->
 *     destinations({
 *         it.toAddresses("example@example.com")
 *           .replacementTemplateData("""{ "name": "Mary" }""")
 *     }, {
 *         it.toAddresses("example2@example.com")
 *           .replacementTemplateData("""{ "name": "John" }""")
 *     })
 *     template("Test Template")
 *     defaultTemplateData("""{ "senderName": "Fred" }""")
 * }
 * ```
 */
fun <T> SesAsyncClient.sendBulkTemplatedEmailFlow(
    upstream: Flow<T>,
    concurrency: Int = 1,
    f: SendBulkTemplatedEmailRequest.Builder.(T) -> Unit
): Flow<SendBulkTemplatedEmailResponse> =
    sendBulkTemplatedEmailFlow(upstream.sendBulkTemplatedEmailRequest(f), concurrency)

/**
 * Sends a bulk templated email for each [SendBulkTemplatedEmailRequest] in the given upstream flow, using this client to send the emails.
 *
 * @param upstream The flow of [SendBulkTemplatedEmailRequest]s to send.
 * @param concurrency The maximum number of concurrent requests. Default is 1.
 * @return A new flow of [SendBulkTemplatedEmailResponse]s.
 *
 * Example usage:
 * ```kotlin
 * val client = SesAsyncClient.create()
 * val requests = flowOf(
 *     SendBulkTemplatedEmailRequest
 *         .builder()
 *         .template("Test Template")
 *         .defaultTemplateData("""{ "senderName": "Fred" }""")
 *         .destinations({
 *             it.toAddresses("example1@example.com")
 *               .replacementTemplateData("""{ "name": "Mary" }""")
 *         }, {
 *             it.toAddresses("example2@example.com")
 *               .replacementTemplateData("""{ "name": "John" }""")
 *        })
 *        .build()
 * )
 *
 * val responseFlow = client.sendBulkTemplatedEmailFlow(requests, 2)
 * ```
 */
fun SesAsyncClient.sendBulkTemplatedEmailFlow(
    upstream: Flow<SendBulkTemplatedEmailRequest>,
    concurrency: Int = 1
): Flow<SendBulkTemplatedEmailResponse> =
    upstream.mapAsync(concurrency) {
        sendBulkTemplatedEmail(it).await()
    }
