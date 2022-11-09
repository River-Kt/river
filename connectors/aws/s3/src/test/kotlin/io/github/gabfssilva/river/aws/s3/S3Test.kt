package io.github.gabfssilva.river.aws.s3

import io.github.gabfssilva.river.core.intersperse
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import kotlinx.coroutines.flow.*
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse
import software.amazon.awssdk.services.s3.model.UploadPartResponse
import java.net.URI

class S3Test : FeatureSpec({
    feature("S3 streaming connector") {
        scenario("Successful upload") {
            val responses =
                s3Client
                    .upload(bucket = "test", "test.txt", flow)
                    .toList()

            val (
                createMultiPart,
                part1,
                part2,
                part3,
                complete
            ) = responses

            createMultiPart.shouldBeTypeOf<CreateMultipartUploadResponse>()

            part1.shouldBeTypeOf<UploadPartResponse>()
            part2.shouldBeTypeOf<UploadPartResponse>()
            part3.shouldBeTypeOf<UploadPartResponse>()

            complete.shouldBeTypeOf<CompleteMultipartUploadResponse>()
        }

        scenario("Successful download") {
            s3Client
                .upload(bucket = "test", "test.txt", flow)
                .collect()

            val (metadata, content) = s3Client.download("test", "test.txt")

            metadata.contentLength() shouldBe ContentLenght

            val count =
                content
                    .map { String(it) }
                    .toList()
                    .fold("") { acc, s -> acc + s }
                    .split("\n")
                    .size

            count shouldBe 800000
        }
    }
})

const val ContentLenght = 12688894L

private val flow =
    (1..800000)
        .asFlow()
        .map { "hello, #$it!" }
        .intersperse("\n")
        .map { it.toByteArray() }

private val s3Client: S3AsyncClient =
    S3AsyncClient
        .builder()
        .endpointOverride(URI("http://s3.localhost.localstack.cloud:4566"))
        .region(Region.US_EAST_1)
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create("x", "x")
            )
        )
        .build()

