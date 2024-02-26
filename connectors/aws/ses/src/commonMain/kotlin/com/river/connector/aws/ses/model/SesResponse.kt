package com.river.connector.aws.ses.model

import aws.sdk.kotlin.services.ses.model.*

/**
 * Represents a sealed interface for different types of SES (Simple Email Service) response models.
 * This interface encapsulates various response types after sending emails using AWS SES.
 *
 * Each class implementing this interface corresponds to a specific type of email sending response,
 * such as responses for raw emails, single emails, single templated emails, and bulk templated emails.
 *
 * @param Resp The type of the response object specific to each SES email sending method.
 */
sealed interface SesResponse<Resp> {
    val response: Resp

    /**
     * Represents the response for a raw email sending request.
     *
     * @param response The SendRawEmailResponse object containing the details of the response for the raw email sent.
     */
    data class Raw(
        override val response: SendRawEmailResponse
    ) : SesResponse<SendRawEmailResponse>

    /**
     * Represents the response for a single email sending request.
     *
     * @param response The SendEmailResponse object containing the details of the response for the single email sent.
     */
    data class Single(
        override val response: SendEmailResponse
    ) : SesResponse<SendEmailResponse>

    /**
     * Represents the response for a single templated email sending request.
     *
     * @param response The SendTemplatedEmailResponse object containing the details of the response for the templated email sent.
     */
    data class SingleTemplated(
        override val response: SendTemplatedEmailResponse
    ) : SesResponse<SendTemplatedEmailResponse>

    /**
     * Represents the response for a bulk templated email sending request.
     *
     * @param response The SendBulkTemplatedEmailResponse object containing the details of the response for the bulk templated emails sent.
     */
    data class BulkTemplated(
        override val response: SendBulkTemplatedEmailResponse
    ) : SesResponse<SendBulkTemplatedEmailResponse>
}
