@file:OptIn(FlowPreview::class)

package io.river.connector.aws.s3

import io.river.core.asByteArray
import io.river.core.chunked
import io.river.core.flowOf
import io.river.core.mapParallel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.future.await
import kotlinx.coroutines.reactive.asFlow
import software.amazon.awssdk.core.async.AsyncRequestBody.fromBytes
import software.amazon.awssdk.core.async.AsyncResponseTransformer
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.CompletedPart.builder
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import software.amazon.awssdk.services.s3.model.S3Response
import java.util.*

private const val MINIMUM_UPLOAD_SIZE = 1024 * 1024 * 5

/**
 * Downloads an S3 object from the specified [bucket] and [key].
 * Returns a [Flow] of a pair containing the [GetObjectResponse] and a [Flow] of [ByteArray] chunks.
 *
 * Example usage:
 *
 * ```
 * s3Client.download("my-bucket", "my-key")
 *     .collect { (response, data) ->
 *         println("Response: $response")
 *         data.collect { chunk ->
 *             println("Received chunk of size ${chunk.size}")
 *             // process chunk data
 *         }
 *     }
 * ```
 */
fun S3AsyncClient.download(
    bucket: String,
    key: String
): Flow<Pair<GetObjectResponse, Flow<ByteArray>>> =
    flowOf {
        getObject({ it.bucket(bucket).key(key) }, AsyncResponseTransformer.toPublisher())
            .await()
            .let { it.response() to it.asFlow().asByteArray() }
    }

/**
 * Uploads the given [upstream] flow of bytes as a multipart upload to the specified [bucket] and [key] in Amazon S3,
 * with the specified [parallelism]. Each chunk of bytes emitted by the [upstream] flow is uploaded as a separate part,
 * with a minimum size of 5 MB.
 *
 * The resulting flow emits [S3Response]s for each upload action, including the initial creation of the multipart upload,
 * the upload of each part, and the completion of the multipart upload.
 *
 * Example usage:
 *
 * ```
 * val myFlow: Flow<Byte> =
 *     "Hello, world! I am going to be on S3 pretty soon!"
 *         .toByteArray(Charsets.UTF_8)
 *         .toList()
 *         .let(::flowOf)
 *         .flatten()
 *
 * val s3AsyncClient = S3AsyncClient.builder().build()
 * val bucket = "my-bucket"
 * val key = "file.txt"
 *
 * s3AsyncClient
 *     .uploadBytes(bucket, key, myFlow)
 *     .collect()
 * ```
 */
fun S3AsyncClient.uploadBytes(
    bucket: String,
    key: String,
    upstream: Flow<Byte>,
    parallelism: Int = 1
): Flow<S3Response> = flow {
    val uploadResponse = createMultipartUpload { it.bucket(bucket).key(key) }.await()
    emit(uploadResponse)
    val uploadId = uploadResponse.uploadId()

    val uploadedParts =
        upstream
            .chunked(MINIMUM_UPLOAD_SIZE)
            .withIndex()
            .mapParallel(parallelism) { (part, chunk) -> uploadPart(bucket, key, uploadId, part, chunk) }
            .onEach { emit(it) }
            .map { it.eTag() }
            .toList()

    emit(
        completeMultipartUpload(
            bucket = bucket,
            key = key,
            uploadId = uploadId,
            etags = uploadedParts
        )
    )
}

/**
 * Uploads the given [upstream] flow of bytes as a multipart upload to the specified [bucket] and [key] in Amazon S3,
 * with the specified [parallelism]. Each chunk of bytes emitted by the [upstream] flow is uploaded as a separate part,
 * with a minimum size of 5 MB.
 *
 * The resulting flow emits [S3Response]s for each upload action, including the initial creation of the multipart upload,
 * the upload of each part, and the completion of the multipart upload.
 *
 * Example usage:
 *
 * ```
 * val myFlow: Flow<Byte> =
 *     "Hello, world! I am going to be on S3 pretty soon!"
 *         .toByteArray(Charsets.UTF_8)
 *         .let(::flowOf)
 *
 * val s3AsyncClient = S3AsyncClient.builder().build()
 * val bucket = "my-bucket"
 * val key = "file.txt"
 *
 * s3AsyncClient
 *     .upload(bucket, key, myFlow)
 *     .collect()
 * ```
 */
fun S3AsyncClient.upload(
    bucket: String,
    key: String,
    upstream: Flow<ByteArray>,
    parallelism: Int = 1
): Flow<S3Response> =
    uploadBytes(
        bucket = bucket,
        key = key,
        upstream = upstream.flatMapConcat { it.toList().asFlow() },
        parallelism = parallelism
    )

private suspend fun S3AsyncClient.uploadPart(
    bucket: String,
    key: String,
    uploadId: String,
    part: Int,
    bytes: List<Byte>
) = uploadPart({
    it.bucket(bucket)
        .key(key)
        .uploadId(uploadId)
        .partNumber(part + 1)
}, fromBytes(bytes.toByteArray())).await()

private suspend fun S3AsyncClient.completeMultipartUpload(
    bucket: String,
    key: String,
    uploadId: String,
    etags: List<String>
) = completeMultipartUpload { builder ->
    builder
        .bucket(bucket)
        .key(key)
        .uploadId(uploadId)
        .multipartUpload { complete ->
            etags
                .mapIndexed { number, part ->
                    builder().apply { partNumber(number + 1).eTag(part) }.build()
                }
                .let { complete.parts(it) }
        }
}.await()
