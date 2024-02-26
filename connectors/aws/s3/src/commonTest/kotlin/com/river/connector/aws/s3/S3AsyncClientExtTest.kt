@file:OptIn(InternalApi::class)

package com.river.connector.aws.s3

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.createBucket
import aws.sdk.kotlin.services.s3.model.ExpressionType
import aws.sdk.kotlin.services.s3.model.JsonType
import aws.sdk.kotlin.services.s3.model.RecordsEvent
import aws.smithy.kotlin.runtime.InternalApi
import aws.smithy.kotlin.runtime.net.url.Url
import aws.smithy.kotlin.runtime.util.PlatformProvider
import com.river.connector.aws.s3.model.S3Response
import com.river.core.GroupStrategy.Count
import com.river.core.flatMapIterable
import com.river.core.intersperse
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.every
import io.mockk.mockkObject
import kotlinx.coroutines.flow.*

class S3AsyncClientExtTest : FunSpec({
    context("Amazon S3 as stream") {
        val bucketName = "test-bucket"
        client.createBucket { bucket = bucketName }

        test("successful upload") {
            val responses =
                client
                    .uploadBytes(bucket = bucketName, "test.txt", flow)
                    .toList()

            val (
                createMultiPart,
                part1,
                part2,
                part3,
                complete
            ) = responses

            createMultiPart.shouldBeTypeOf<S3Response.CreateMultipartUpload>()

            part1.shouldBeTypeOf<S3Response.UploadPart>()
            part2.shouldBeTypeOf<S3Response.UploadPart>()
            part3.shouldBeTypeOf<S3Response.UploadPart>()

            complete.shouldBeTypeOf<S3Response.CompleteMultipartUpload>()
        }

        test("successful download") {
            client
                .uploadBytes(bucket = bucketName, "test.txt", flow)
                .collect()

            client
                .download {
                    bucket = bucketName
                    key = "test.txt"
                }
                .collect { (metadata, content) ->
                    metadata.contentLength shouldBe ContentLength

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

        test("successful upload split") {
            val responses =
                client
                    .uploadSplit(
                        bucket = bucketName,
                        upstream = flow,
                        splitStrategy = Count(1024 * 1024 * 5)
                    ) { "file-$it.txt" }
                    .toList()

            val (
                piece1,
                piece2,
                piece3,
            ) = responses

            piece1.shouldBeTypeOf<S3Response.PutObject>()
            piece2.shouldBeTypeOf<S3Response.PutObject>()
            piece3.shouldBeTypeOf<S3Response.PutObject>()
        }

        test("successful upload many files + merge content") {
            client
                .uploadSplit(
                    bucket = bucketName,
                    upstream = flow,
                    splitStrategy = Count(1024 * 1024 * 5)
                ) { "file-$it.txt" }
                .collect()

            client
                .mergeContents(
                    files = listOf(
                        bucketName to "file-1.txt",
                        bucketName to "file-2.txt",
                        bucketName to "file-3.txt"
                    )
                ) {
                    bucket = bucketName
                    key = "file-all.txt"
                }.collect()

            client
                .download {
                    bucket = bucketName
                    key = "test.txt"
                }
                .collect { (metadata, content) ->
                    metadata.contentLength shouldBe ContentLength

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

        // Disabled since it's Localstack Pro only
        xtest("querying data using selectObjectContent") {
            val items = 100

            client
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
                        .map { it.encodeToByteArray() }
                )
                .collect()

            client
                .selectObjectContent {
                    bucket = bucketName
                    key = "users.jsonl"

                    expression = "SELECT * FROM S3Object s where s.id = 1"
                    expressionType = ExpressionType.Sql

                    inputSerialization { json { type = JsonType.Lines } }
                    outputSerialization { json { } }
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
        .flatMapIterable { it.encodeToByteArray().toList() }
        .take(1024 * 1024 * 15)

val client: S3Client by lazy {
    lateinit var client: S3Client

    mockkObject(PlatformProvider.System) {
        every { PlatformProvider.System.isAndroid } returns false

        client = S3Client {
            endpointUrl = Url.parse("http://s3.localhost.localstack.cloud:4566")
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
