package com.river.connector.aws.ses.model

import aws.sdk.kotlin.services.ses.model.*

/**
 * Represents a sealed interface for different types of SES (Simple Email Service) request models.
 * This interface encapsulates various request types for sending emails using AWS SES.
 *
 * Each class implementing this interface corresponds to a specific type of email sending request,
 * such as sending raw emails, single emails, single templated emails, and bulk templated emails.
 *
 * @param Req The type of the request object specific to each SES email sending method.
 */
sealed interface SesRequest<Req> {
    val request: Req

    /**
     * Represents a request to send a raw email.
     *
     * @param request The SendRawEmailRequest object containing the details of the raw email to be sent.
     */
    data class Raw(
        override val request: SendRawEmailRequest
    ) : SesRequest<SendRawEmailRequest> {
        companion object {
            operator fun invoke(request: SendRawEmailRequest.Builder.() -> Unit) =
                Raw(SendRawEmailRequest { request() })
        }
    }

    /**
     * Represents a request to send a single email.
     *
     * @param request The SendEmailRequest object containing the details of the email to be sent.
     */
    data class Single(
        override val request: SendEmailRequest
    ) : SesRequest<SendEmailRequest> {
        companion object {
            operator fun invoke(request: SendEmailRequest.Builder.() -> Unit) =
                Single(SendEmailRequest { request() })
        }
    }

    /**
     * Represents a request to send a single templated email.
     *
     * @param request The SendTemplatedEmailRequest object containing the details of the templated email to be sent.
     */
    data class SingleTemplated(
        override val request: SendTemplatedEmailRequest
    ) : SesRequest<SendTemplatedEmailRequest> {
        companion object {
            operator fun invoke(request: SendTemplatedEmailRequest.Builder.() -> Unit) =
                SingleTemplated(SendTemplatedEmailRequest { request() })
        }
    }

    /**
     * Represents a request to send bulk templated emails.
     *
     * @param request The SendBulkTemplatedEmailRequest object containing the details for sending multiple templated emails.
     */
    data class BulkTemplated(
        override val request: SendBulkTemplatedEmailRequest
    ) : SesRequest<SendBulkTemplatedEmailRequest> {
        companion object {
            operator fun invoke(request: SendBulkTemplatedEmailRequest.Builder.() -> Unit) =
                BulkTemplated(SendBulkTemplatedEmailRequest { request() })
        }
    }
}
