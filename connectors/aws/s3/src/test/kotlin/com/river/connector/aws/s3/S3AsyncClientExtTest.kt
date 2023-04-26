@file:OptIn(FlowPreview::class)

package com.river.connector.aws.s3

import com.river.core.intersperse
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.future.await
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse
import software.amazon.awssdk.services.s3.model.PutObjectResponse
import software.amazon.awssdk.services.s3.model.UploadPartResponse
import java.net.URI

class S3Test : FeatureSpec({
    feature("S3 streaming connector") {
        s3Client.createBucket { it.bucket("test") }.await()

        scenario("Successful upload") {
            val responses =
                s3Client
                    .uploadBytes(bucket = "test", "test.txt", flow)
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
                .uploadBytes(bucket = "test", "test.txt", flow)
                .collect()

            val (metadata, content) = s3Client.download("test", "test.txt").first()
            metadata.contentLength() shouldBe ContentLenght

            val count =
                content
                    .map { String(it) }
                    .toList()
                    .fold("") { acc, s -> acc + s }
                    .split("\n")
                    .size

            count shouldBe 2104969
        }

        scenario("Successful many files upload") {
            val responses =
                s3Client
                    .uploadSplit(bucket = "test", flow, splitEach = 1024 * 1024 * 5) {
                        "file-$it.txt"
                    }
                    .toList()

            val (
                piece1,
                piece2,
                piece3,
            ) = responses

            piece1.shouldBeTypeOf<PutObjectResponse>()
            piece2.shouldBeTypeOf<PutObjectResponse>()
            piece3.shouldBeTypeOf<PutObjectResponse>()
        }

        scenario("Successful many files upload and merge them into one") {
            s3Client
                .uploadSplit(bucket = "test", upstream = flow, splitEach = 1024 * 1024 * 5) {
                    "file-$it.txt"
                }.collect()

            s3Client.multipartUploadCopy(
                bucket = "test",
                key = "file-all.txt",
                files = flowOf(
                    "test" to "file-1.txt",
                    "test" to "file-2.txt",
                    "test" to "file-3.txt"
                )
            ).collect()

            val (metadata, content) = s3Client.download("test", "file-all.txt").first()

            metadata.contentLength() shouldBe ContentLenght

            val count =
                content
                    .map { String(it) }
                    .toList()
                    .fold("") { acc, s -> acc + s }
                    .split("\n")
                    .size

            count shouldBe 2104969
        }
    }
})

const val ContentLenght = 15728640L

private val flow =
    (1..Long.MAX_VALUE)
        .asFlow()
        .map { it.toString() }
        .intersperse("\n")
        .map { it.toByteArray() }
        .flatMapConcat { it.asList().asFlow() }
        .take(1024 * 1024 * 15)

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
