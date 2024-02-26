@file:OptIn(InternalApi::class)

package com.river.connector.aws.lambda

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.lambda.LambdaClient
import aws.smithy.kotlin.runtime.InternalApi
import aws.smithy.kotlin.runtime.net.url.Url
import aws.smithy.kotlin.runtime.util.PlatformProvider
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkObject
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.single

class LambdaExtKtTest : FeatureSpec({
    feature("Lambda invocation") {
        scenario("Successful invocation") {
            val response =
                client
                    .invokeFlow(flowOf("""{"message":"my name is gabs"}""")) {
                        functionName = "hello_world"
                        payload = it.encodeToByteArray()
                    }
                    .single()

            with(response) {
                statusCode shouldBe 200

                String(checkNotNull(payload))
                    .replace("\n", "") shouldBe
                    """{"message": "hello, world! your message was my name is gabs"}"""
            }
        }
    }
})

val client: LambdaClient by lazy {
    lateinit var client: LambdaClient

    mockkObject(PlatformProvider.System) {
        every { PlatformProvider.System.isAndroid } returns false

        client = LambdaClient {
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
