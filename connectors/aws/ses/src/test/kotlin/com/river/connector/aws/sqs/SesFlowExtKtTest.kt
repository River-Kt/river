package com.river.connector.aws.sqs

import com.river.connector.aws.ses.model.*
import com.river.connector.aws.ses.sendEmailFlow
import com.river.core.chunked
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.future.await
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.ses.SesAsyncClient
import java.net.URI
import java.util.UUID.randomUUID

class SesFlowExtKtTest : FeatureSpec({
    val numberOfMessages = 100
    val from = "from@gmail.com"

    beforeSpec {
        sesAsyncClient.verifyEmailAddress { it.emailAddress(from) }.await()
    }

    feature("Amazon SES - Simple E-mail Service") {
        scenario("Send e-mail") {
            val currentSentEmails = currentQuota()

            val emails =
                (1..numberOfMessages)
                    .asFlow()
                    .map { "This is the e-mail #$it" }
                    .map { body ->
                        SendEmailRequest.Single(
                            source = Source.Plain("from@gmail.com"),
                            destination = Destination("to@gmail.com"),
                            message = Message.Text(
                                body = Content(body),
                                subject = Content("Test e-mail")
                            )
                        )
                    }

            sesAsyncClient
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
                        SendEmailRequest.Single(
                            source = Source.Plain("from@gmail.com"),
                            destination = Destination("to@gmail.com"),
                            message = Message.Template(
                                data = """{ "senderName": "$sender", "name": "${user.name}" }""",
                                ref = Message.Template.TemplateRef.Name(templateName)
                            )
                        )
                    }

            sesAsyncClient
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
                        SendEmailRequest.Single(
                            source = Source.Plain("from@gmail.com"),
                            destination = Destination(users.map { it.email }),
                            message = Message.Template(
                                data = """{ "senderName": "$sender" }""",
                                ref = Message.Template.TemplateRef.Name(templateName)
                            )
                        )
                    }

            sesAsyncClient
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
                        SendEmailRequest.BulkTemplated(
                            source = Source.Plain("from@gmail.com"),
                            destinations = users.map {
                                TemplatedDestination(
                                    destination = Destination(it.email),
                                    replacementData = """{ "name": "${it.name}" }"""
                                )
                            },
                            message = Message.Template(
                                data = """{ "senderName": "$sender" }""",
                                ref = Message.Template.TemplateRef.Name(templateName)
                            )
                        )
                    }

            sesAsyncClient
                .sendEmailFlow(emails)
                .collect()

            currentQuota() shouldBe currentSentEmails + numberOfMessages
        }
    }
})

suspend fun currentQuota(): Int =
    sesAsyncClient
        .sendQuota
        .await()
        .sentLast24Hours()
        .toInt()

suspend fun createTemplate(
    name: String,
    subject: String,
    content: String
) = sesAsyncClient.createTemplate {
    it.template { template ->
        template
            .subjectPart(subject)
            .templateName(name)
            .textPart(content)
    }
}.await()

val sesAsyncClient: SesAsyncClient =
    SesAsyncClient
        .builder()
        .endpointOverride(URI("http://localhost:4566"))
        .region(Region.US_EAST_1)
        .credentialsProvider {
            AwsBasicCredentials.create("x", "x")
        }
        .build()

fun shortId() = randomUUID().toString().replace("-", "").substring(0, 10)
