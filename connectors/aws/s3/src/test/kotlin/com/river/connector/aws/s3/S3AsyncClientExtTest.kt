package com.river.connector.aws.s3

import com.river.core.*
import com.river.core.GroupStrategy.Count
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.future.await
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.*
import java.net.URI

class S3AsyncClientExtTest : FeatureSpec({
    feature("S3 streaming connector") {
        val bucketName = "test"
        s3Client.createBucket { it.bucket(bucketName) }.await()

        scenario("Successful upload") {
            val responses =
                s3Client
                    .uploadBytes(bucket = bucketName, "test.txt", flow)
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
                .uploadBytes(bucket = bucketName, "test.txt", flow)
                .collect()

            val (metadata, content) = s3Client.download(bucketName, "test.txt").first()
            metadata.contentLength() shouldBe ContentLength

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
                    .uploadSplit(bucket = bucketName, upstream = flow, splitStrategy = Count(1024 * 1024 * 5)) {
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
                .uploadSplit(bucket = bucketName, upstream = flow, splitStrategy = Count(1024 * 1024 * 5)) {
                    "file-$it.txt"
                }.collect()

            s3Client.mergeContents(
                bucket = bucketName,
                key = "file-all.txt",
                files = listOf(
                    bucketName to "file-1.txt",
                    bucketName to "file-2.txt",
                    bucketName to "file-3.txt"
                )
            ).collect()

            val (metadata, content) = s3Client.download(bucketName, "file-all.txt").first()

            metadata.contentLength() shouldBe ContentLength

            val count =
                content
                    .map { String(it) }
                    .toList()
                    .fold("") { acc, s -> acc + s }
                    .split("\n")
                    .size

            count shouldBe 2104969
        }

        // Localstack seems to not like selectObjectContent very much,
        // the following example should return the first item, but it doesn't.
        // I tested directly against AWS S3, and, it works just fine, so I assume it's probably a Localstack bug.
        // Also, Localstack prints the following error to the console:
        // ProtocolSerializerError: Expected iterator for streaming event serialization.
        scenario("Querying data using selectObjectContent") {
            val items = 100

            s3Client
                .upload(
                    bucket = bucketName,
                    key = "users.jsonl",
                    upstream =
                        (1..items)
                            .asFlow()
                            .map {
                                val age = if (it % 2 == 0) "32" else "17"
                                val name = "name-$it"
                                """{"id":$it,"name":"$name","age":$age}"""
                            }
                            .intersperse("\n")
                            .asByteArray()
                )
                .collect()

            s3Client
                .selectObjectContent {
                    bucket(bucketName)
                    key("users.jsonl")

                    expression("SELECT * FROM S3Object s where s.id = 1")
                    expressionType(ExpressionType.SQL)

                    inputSerialization { s -> s.json { it.type(JSONType.LINES) } }
                    outputSerialization { it.json { } }
                }
                .filterIsInstance<RecordsEvent>()
                .collect()
        }
    }
})

const val ContentLength = 15728640L

private val flow =
    (1..Long.MAX_VALUE)
        .asFlow()
        .map { it.toString() }
        .intersperse("\n")
        .asBytes()
        .take(1024 * 1024 * 15)

private val s3Client: S3AsyncClient =
    S3AsyncClient
        .builder()
        .endpointOverride(URI("http://s3.localhost.localstack.cloud:4566"))
        .region(Region.US_EAST_1)
        .credentialsProvider {
            AwsBasicCredentials.create(
                "x",
                "x"
            )
        }
        .build()
