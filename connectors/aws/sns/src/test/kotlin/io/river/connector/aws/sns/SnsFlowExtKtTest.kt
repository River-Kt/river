package io.river.connector.aws.sns

import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.river.core.flatten
import kotlinx.coroutines.flow.*
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
                val totalSize = 20

                val flow = (1..totalSize).asFlow().map {
                    PublishRequestEntry(it.toString())
                }

                snsClient
                    .publishFlow(topic, flow)
                    .map { it.successful() }
                    .flatten()
                    .toList() shouldHaveSize totalSize
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
