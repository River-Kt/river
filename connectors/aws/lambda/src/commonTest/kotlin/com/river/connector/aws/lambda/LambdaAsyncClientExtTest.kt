package com.river.connector.aws.lambda

import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.single
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.lambda.LambdaAsyncClient
import java.net.URI

class LambdaExtKtTest : FeatureSpec({
    feature("Lambda invocation") {
        scenario("Successful invocation") {
            val response =
                client
                    .invokeFlow(
                        functionName = "hello_world",
                        upstream = flowOf("""{"message":"my name is gabs"}""")
                    )
                    .single()

            with(response) {
                statusCode() shouldBe 200

                payload()
                    .asUtf8String()
                    .replace("\n", "") shouldBe
                    """{"message": "hello, world! your message was my name is gabs"}"""
            }
        }
    }
})

val client: LambdaAsyncClient =
    LambdaAsyncClient
        .builder()
        .endpointOverride(URI("http://localhost:4566"))
        .region(Region.US_EAST_1)
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create("x", "x")
            )
        )
        .build()
