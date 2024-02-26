@file:Suppress("UNCHECKED_CAST")

package com.river.connector.aws.ses

import aws.sdk.kotlin.services.ses.SesClient
import com.river.connector.aws.ses.model.SesRequest
import com.river.connector.aws.ses.model.SesResponse
import com.river.core.mapAsync
import kotlinx.coroutines.flow.Flow

/**
 * Sends a flow of email requests using the AWS Simple Email Service (SES) client.
 *
 * This extension function on SesClient takes an upstream Flow of SesRequest objects, processes them concurrently,
 * and emits a Flow of SesResponse objects corresponding to the results of the email sending operations.
 *
 * The function supports different types of SES requests, including bulk templated, raw, single, and single templated emails.
 *
 * @param R The type parameter representing the payload type of the SesRequest.
 * @param upstream A Flow of SesRequest objects that represent the email sending requests.
 * @param concurrency An integer defining the level of concurrency for processing the requests. Default is 1, meaning sequential processing.
 *
 * @return A Flow of SesResponse objects, each representing the outcome of an email sending request.
 *
 * Example usage:
 * ```
 * val sesClient = SesClient { region = "us-east-1" }
 * val emailRequestsFlow = flowOf(
 *     SesRequest.SingleTemplated(SesSingleTemplatedRequest(...)),
 *     SesRequest.BulkTemplated(SesBulkTemplatedRequest(...)),
 *     // more requests
 * )
 *
 * val emailResponsesFlow = sesClient.sendEmailFlow(emailRequestsFlow, concurrency = 2)
 *
 * emailResponsesFlow
 *     .collect { response ->
 *         // Handle each response
 *     }
 * ```
 */
fun <R> SesClient.sendEmailFlow(
    upstream: Flow<SesRequest<R>>,
    concurrency: Int = 1,
): Flow<SesResponse<*>> =
    upstream
        .mapAsync(concurrency) {
            when (it) {
                is SesRequest.BulkTemplated -> SesResponse.BulkTemplated(sendBulkTemplatedEmail(it.request))
                is SesRequest.Raw -> SesResponse.Raw(sendRawEmail(it.request))
                is SesRequest.Single -> SesResponse.Single(sendEmail(it.request))
                is SesRequest.SingleTemplated -> SesResponse.SingleTemplated(sendTemplatedEmail(it.request))
            }
        }
