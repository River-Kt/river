@file:OptIn(InternalApi::class)

package com.river.connector.aws.sqs

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.ses.SesClient
import aws.sdk.kotlin.services.ses.createTemplate
import aws.sdk.kotlin.services.ses.model.BulkEmailDestination
import aws.sdk.kotlin.services.ses.model.Destination
import aws.sdk.kotlin.services.ses.verifyEmailAddress
import aws.smithy.kotlin.runtime.InternalApi
import aws.smithy.kotlin.runtime.net.url.Url
import aws.smithy.kotlin.runtime.util.PlatformProvider
import com.river.connector.aws.ses.model.*
import com.river.connector.aws.ses.sendEmailFlow
import com.river.core.chunked
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import java.util.UUID.randomUUID

class SesFlowExtKtTest : FeatureSpec({
    val numberOfMessages = 100
    val from = "from@gmail.com"

    beforeSpec {
        sesClient.verifyEmailAddress { emailAddress = from }
    }

    feature("Amazon SES - Simple E-mail Service") {
        scenario("Send e-mail") {
            val currentSentEmails = currentQuota()

            val emails =
                (1..numberOfMessages)
                    .asFlow()
                    .map { "This is the e-mail #$it" }
                    .map { contentBody ->
                        SesRequest.Single {
                            source = "from@gmail.com"
                            destination { toAddresses = listOf("to@gmail.com") }
                            message {
                                subject { data = "Test e-mail" }
                                body { text { data = contentBody } }
                            }
                        }

                    }

            sesClient
                .sendEmailFlow(emails)
                .collect()

            currentQuota() shouldBe currentSentEmails + numberOfMessages
        }

        data class User(
            val name: String,
            val email: String
        )

        val users: Flow<User> =
            (1..numberOfMessages)
                .asFlow()
                .map { User("user_$it", "user_$it@gmail.com") }

        scenario("Send templated e-mail") {
            val templateName = "how_can_i_help_${shortId()}"
            val sender = shortId()

            createTemplate(
                name = templateName,
                subject = "What's up, {{name}}?",
                content = "Hello, {{name}}! My name is {{senderName}} and I am here to help you"
            )

            val currentSentEmails = currentQuota()

            val emails =
                users
                    .map { user ->
                        SesRequest.SingleTemplated {
                            source = "from@gmail.com"
                            destination { toAddresses = listOf("to@gmail.com") }
                            template = templateName
                            templateData ="""{ "senderName": "$sender", "name": "${user.name}" }"""
                        }
                    }

            sesClient
                .sendEmailFlow(emails)
                .collect()

            currentQuota() shouldBe currentSentEmails + numberOfMessages
        }

        scenario("Send templated e-mail to multiple destinations") {
            val templateName = "how_can_i_help_${shortId()}"
            val sender = shortId()

            createTemplate(
                name = templateName,
                subject = "What's up?",
                content = "Hello! My name is {{senderName}} and I am here to help you."
            )

            val currentSentEmails = currentQuota()

            val emails =
                users
                    .chunked(10)
                    .map { users ->
                        SesRequest.SingleTemplated {
                            source = "from@gmail.com"
                            destination { toAddresses = users.map { it.email } }
                            template = templateName
                            templateData = """{ "senderName": "$sender" }"""
                        }
                    }

            sesClient
                .sendEmailFlow(emails)
                .collect()

            currentQuota() shouldBe currentSentEmails + numberOfMessages
        }

        scenario("Send bulk templated e-mail") {
            val templateName = "how_can_i_help_${shortId()}"
            val sender = shortId()

            createTemplate(
                name = templateName,
                subject = "What's up, {{name}}?",
                content = "Hello, {{name}}! My name is {{senderName}} and I am here to help you."
            )

            val currentSentEmails = currentQuota()

            val emails =
                users
                    .chunked(10)
                    .map { users ->
                        SesRequest.BulkTemplated {
                            source = "from@gmail.com"
                            destinations = users.map {
                                BulkEmailDestination {
                                    destination = Destination { toAddresses = listOf(it.email) }
                                    replacementTemplateData = """{ "name": "${it.name}" }"""
                                }
                            }
                            defaultTemplateData = """{ "senderName": "$sender" }"""
                            template = templateName
                        }
                    }

            sesClient
                .sendEmailFlow(emails)
                .collect()

            currentQuota() shouldBe currentSentEmails + numberOfMessages
        }
    }
})

suspend fun currentQuota(): Int =
    sesClient
        .getSendQuota()
        .sentLast24Hours
        .toInt()

suspend fun createTemplate(
    name: String,
    subject: String,
    content: String
) = sesClient.createTemplate {
    template {
        subjectPart = subject
        templateName = name
        textPart = content
    }
}

fun shortId() = randomUUID().toString().replace("-", "").substring(0, 10)

val sesClient: SesClient by lazy {
    lateinit var client: SesClient

    mockkObject(PlatformProvider.System) {
        every { PlatformProvider.System.isAndroid } returns false

        client = SesClient {
            endpointUrl = Url.parse("http://s3.localhost.localstack.cloud:4566")
            region = "us-east-1"
            credentialsProvider = StaticCredentialsProvider {
                accessKeyId = "x"
                secretAccessKey = "x"
            }
            applicationId = "x"
        }
    }

    client
}
