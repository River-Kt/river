package io.river.connector.aws.sqs

import io.kotest.core.spec.style.FeatureSpec
import io.river.core.via
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sesv2.SesV2AsyncClient
import java.net.URI

class SqsFlowExtKtTest : FeatureSpec({
    with(sesAsyncClient) {
        //SES V2 is only supported by Localstack PRO
        //We need to find another way to test it
        xfeature("E-mail publishing flow") {
            scenario("E-mail publishing") {
                (0..10)
                    .asFlow()
                    .map { "This is the e-mail #$it" }
                    .asSendEmailRequest { content ->
                        destination { it.toAddresses("+5511837265361") }

                        content {
                            it.simple {
                                it.subject { it.data("Test e-mail") }

                                it.body {
                                    it.text { it.data(content) }
                                }
                            }
                        }

                        fromEmailAddress("+5511837265364")
                    }
                    .via { sendEmailFlow() }
                    .collect()
            }
        }
    }
})

val sesAsyncClient: SesV2AsyncClient =
    SesV2AsyncClient
        .builder()
        .endpointOverride(URI("http://localhost:4566"))
        .region(Region.US_EAST_1)
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create("x", "x")
            )
        )
        .build()
