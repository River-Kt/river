@file:Suppress("UNCHECKED_CAST")

package com.river.connector.aws.ses

import com.river.connector.aws.ses.internal.*
import com.river.connector.aws.ses.model.*

import com.river.core.mapAsync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.future.await

import software.amazon.awssdk.services.ses.SesAsyncClient
import software.amazon.awssdk.services.ses.model.SesResponse

/**
 * Sends emails using the requests provided in a [Flow]. The emails are sent concurrently with the specified concurrency level.
 *
 * @param upstream The [Flow] of [SendEmailRequest] objects representing the email sending requests.
 * @param concurrency The maximum number of concurrent requests. Defaults to 1.
 * @return A [Flow] of [SesResponse] objects representing the responses to the email sending requests.
 *
 * Example usage:
 *
 * ```kotlin
 * val client = SesAsyncClient.create()
 * val requests = flowOf(SendEmailRequest.Single(...), SendEmailRequest.BulkTemplated(...))
 * val responses = client.sendEmailFlow(requests, concurrency = 2)
 * responses.collect { response ->
 *     println("Response: $response")
 * }
 * ```
 */
fun SesAsyncClient.sendEmailFlow(
    upstream: Flow<SendEmailRequest>,
    concurrency: Int = 1,
): Flow<SesResponse> =
    upstream.mapAsync(concurrency) { coSendEmail(it) }

suspend fun SesAsyncClient.coSendEmail(
    request: SendEmailRequest
): SesResponse =
    when (request) {
        is SendEmailRequest.BulkTemplated -> {
            sendBulkTemplatedEmail(request.asSesBulkTemplatedRequest()).await()
        }

        is SendEmailRequest.Single<*> ->
            when (request.message) {
                is Message.Plain -> {
                    request as SendEmailRequest.Single<Message.Plain>
                    sendEmail(request.asSesSendEmailRequest()).await()
                }

                is Message.Template -> {
                    request as SendEmailRequest.Single<Message.Template>
                    sendTemplatedEmail(request.asSesTemplatedRequest()).await()
                }
            }
    }
