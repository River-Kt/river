@file:OptIn(InternalApi::class)

package com.river.connector.aws.sns

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.sns.SnsClient
import aws.sdk.kotlin.services.sns.model.CreateTopicRequest
import aws.smithy.kotlin.runtime.InternalApi
import aws.smithy.kotlin.runtime.net.url.Url
import aws.smithy.kotlin.runtime.util.PlatformProvider
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkObject
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.count

class SnsClientExtTest : FeatureSpec({
    feature("SNS publish flow") {
        with(client) {
            scenario("Successful message publishing") {
                val totalSize = 20

                val arn =
                    createTopic(CreateTopicRequest { name = "topic_sample" })
                        .topicArn
                        .orEmpty()

                (1..totalSize)
                    .asFlow()
                    .publishFlow(arn) { number ->
                        message = "$number"
                    }
                    .count() shouldBe totalSize
            }
        }
    }
})

val client: SnsClient by lazy {
    lateinit var client: SnsClient

    mockkObject(PlatformProvider.System) {
        every { PlatformProvider.System.isAndroid } returns false

        client = SnsClient {
            endpointUrl = Url.parse("http://localhost:4566")
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
