package com.river.connector.aws.sqs

import com.river.connector.aws.ses.mapSendEmailRequest
import com.river.connector.aws.ses.sendEmailFlow
import io.kotest.core.spec.style.FeatureSpec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sesv2.SesV2AsyncClient
import java.net.URI

class SqsFlowExtKtTest : FeatureSpec({
    xfeature("E-mail publishing flow") {
        scenario("E-mail publishing") {
            val numberOfMessages = 100

            val messages: Flow<String> =
                (1..numberOfMessages)
                    .asFlow()
                    .map { "This is the e-mail #$it" }

            val emails =
                messages
                    .mapSendEmailRequest { body ->
                        destination { it.toAddresses("to@gmail.com") }

                        content { content ->
                            content.simple { s ->
                                s.subject { it.data("Test e-mail") }

                                s.body {
                                    it.text { it.data(body) }
                                }
                            }
                        }

                        fromEmailAddress("from@gmail.com")
                    }

            (0..10)
                .asFlow()
                .map { "This is the e-mail #$it" }
                .mapSendEmailRequest { content ->

                }
                .let { sesAsyncClient.sendEmailFlow(it) }
                .collect()
        }
    }
})

val sesAsyncClient: SesV2AsyncClient =
    SesV2AsyncClient
        .builder()
        .endpointOverride(URI("http://localhost:4566"))
        .region(Region.US_EAST_1)
        .credentialsProvider {
            AwsBasicCredentials.create("x", "x")
        }
        .build()
