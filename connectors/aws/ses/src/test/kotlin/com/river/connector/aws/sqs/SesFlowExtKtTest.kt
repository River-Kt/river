package com.river.connector.aws.sqs

import com.river.connector.aws.ses.*
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
import software.amazon.awssdk.services.ses.model.BulkEmailDestination
import java.net.URI
import java.util.UUID.randomUUID
import java.util.function.Consumer

class SesFlowExtKtTest : FeatureSpec({
    val numberOfMessages = 100
    val from = "from@gmail.com"

    beforeSpec {
        sesAsyncClient.verifyEmailAddress { it.emailAddress(from) }.await()
    }

    feature("Amazon SES - Simple E-mail Service") {
        scenario("Send e-mail") {
            val currentSentEmails = currentQuota()

            val messages: Flow<String> =
                (1..numberOfMessages)
                    .asFlow()
                    .map { "This is the e-mail #$it" }

            val emails =
                messages.sendEmailRequest { emailContent ->
                    destination { it.toAddresses("to@gmail.com") }
                    source("from@gmail.com")

                    message { message ->
                        message.subject {
                            it.data("Test e-mail")
                        }

                        message.body { body ->
                            body.text { it.data(emailContent) }
                        }
                    }
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
                users.sendTemplatedEmailRequest { user ->
                    destination { it.toAddresses(user.email) }
                    source("from@gmail.com")
                    template(templateName)
                    templateData("""{ "senderName": "$sender", "name": "${user.name}" }""")
                }

            sesAsyncClient
                .sendTemplatedEmailFlow(emails)
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

            sesAsyncClient
                .sendTemplatedEmailFlow(users.chunked(10)) { users ->
                    destination { it.toAddresses(users.map { it.email }) }
                    source("from@gmail.com")
                    template(templateName)
                    templateData("""{ "senderName": "$sender" }""")
                }
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

            sesAsyncClient
                .sendBulkTemplatedEmailFlow(users.chunked(10)) { users ->
                    val destinations = users.map { user ->
                        Consumer<BulkEmailDestination.Builder> { builder ->
                            builder
                                .destination { it.toAddresses(user.email) }
                                .replacementTemplateData("""{ "name": "${user.name}" }""")
                        }
                    }

                    destinations(*destinations.toTypedArray())
                    source("from@gmail.com")
                    template(templateName)
                    defaultTemplateData("""{ "senderName": "$sender" }""")
                }
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
