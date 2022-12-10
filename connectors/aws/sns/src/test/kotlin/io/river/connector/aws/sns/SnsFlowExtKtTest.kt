package io.river.connector.aws.sns

import io.kotest.core.spec.style.FeatureSpec
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.future.await
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sns.SnsAsyncClient
import java.net.URI

class SnsFlowExtKtTest : FeatureSpec({
    feature("SNS publish flow") {
        with(snsClient) {
            val topic = createTopic { it.name("topic_sample") }.await().topicArn()

            scenario("Successful message publishing") {
                val flow = (1..20).asFlow().map {
                    PublishRequestEntry(it.toString())
                }

                snsClient.publishFlow(topic, flow).collect(::println)
            }
        }
    }

})

val snsClient: SnsAsyncClient =
    SnsAsyncClient
        .builder()
        .endpointOverride(URI("http://localhost:4566"))
        .region(Region.US_EAST_1)
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create("x", "x")
            )
        )
        .build()