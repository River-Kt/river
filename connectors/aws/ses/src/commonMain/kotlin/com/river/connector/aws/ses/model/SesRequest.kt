package com.river.connector.aws.ses.model

import java.nio.charset.Charset

/**
 * Represents a request to send an email. It can be either a single email or a bulk templated email.
 */
sealed interface SendEmailRequest {
    /**
     * Represents a single email sending request.
     *
     * @property source The [Source] of the email.
     * @property destination The [Destination] of the email.
     * @property message The [Message] content of the email.
     * @property tags The optional tags of the email.
     * @property replyToAddresses The optional list of reply-to addresses.
     * @property returnPath The optional return path.
     * @property configurationSetName The optional configuration set name.
     */
    data class Single<T : Message>(
        val source: Source,
        val destination: Destination,
        val message: T,
        val tags: Map<String, String> = emptyMap(),
        val replyToAddresses: List<String> = emptyList(),
        val returnPath: ReturnPath? = null,
        val configurationSetName: String? = null
    ) : SendEmailRequest

    /**
     * Represents a bulk email sending request with templates.
     *
     * @property source The [Source] of the email.
     * @property destinations The list of [TemplatedDestination] of the email.
     * @property message The templated [Message] content of the email.
     * @property defaultTags The optional tags of the email.
     * @property replyToAddresses The optional list of reply-to addresses.
     * @property returnPath The optional return path.
     * @property configurationSetName The optional configuration set name.
     */
    data class BulkTemplated(
        val source: Source,
        val destinations: List<TemplatedDestination>,
        val message: Message.Template,
        val defaultTags: Map<String, String> = emptyMap(),
        val replyToAddresses: List<String> = emptyList(),
        val returnPath: ReturnPath? = null,
        val configurationSetName: String? = null,
    ) : SendEmailRequest
}

/**
 * Represents the destination of a templated email in a bulk sending request.
 *
 * @property replacementData The custom template data.
 * @property destination The [Destination] of the email.
 * @property replacementTags The optional tags of the email.
 */
data class TemplatedDestination(
    val replacementData: String,
    val destination: Destination,
    val replacementTags: Map<String, String> = emptyMap(),
)

/**
 * Represents the return path for the email.
 *
 * This could be either a plain source or an Amazon Resource Name (ARN).
 */
sealed interface ReturnPath {
    data class Plain(val path: String) : ReturnPath
    data class Arn(val arn: String) : ReturnPath
}

/**
 * Represents the source of the email.
 *
 * This could be either a plain source or an Amazon Resource Name (ARN).
 */
sealed interface Source {
    data class Plain(val source: String) : Source
    data class Arn(val arn: String) : Source
}

/**
 * Represents the content of the email message.
 *
 * This could be a plain message or a template message.
 */
sealed interface Message {
    sealed interface Plain : Message {
        val subject: Content
        val body: Content
    }

    data class Text(
        override val subject: Content,
        override val body: Content
    ) : Plain

    data class Html(
        override val subject: Content,
        override val body: Content
    ) : Plain

    data class Template(
        val ref: TemplateRef,
        val data: String
    ) : Message {
        sealed interface TemplateRef {
            data class Name(val name: String) : TemplateRef
            data class Arn(val arn: String) : TemplateRef
        }
    }
}

/**
 * Represents the content of the email.
 *
 * @property data The actual data or content of the email.
 * @property charset The character set of the email.
 */
data class Content(
    val data: String,
    val charset: Charset = Charset.defaultCharset()
)

/**
 * Represents the destination of the email.
 *
 * @property to The list of primary recipients.
 * @property cc The list of CC recipients.
 * @property bcc The list of BCC recipients.
 */
data class Destination(
    val to: List<String>,
    val cc: List<String> = emptyList(),
    val bcc: List<String> = emptyList()
) {
    companion object {
        operator fun invoke(vararg to: String): Destination =
            Destination(to.toList())
    }
}
