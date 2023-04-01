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
 * Creates a flow that downloads a file from an Amazon S3 bucket.
 *
 * This function takes a [bucket] and [key] and returns a [Flow] of pairs containing the
 * [GetObjectResponse] and a flow of byte arrays.
 *
 * @param bucket The name of the S3 bucket.
 * @param key The key of the file to download.
 * @return A [Flow] of pairs containing the [GetObjectResponse] and a flow of byte arrays.
 *
 * Example usage:
 *
 * ```
 * val s3Client: S3AsyncClient = ...
 * val bucket = "my-bucket"
 * val key = "path/to/myfile.txt"
 *
 * s3Client.download(bucket, key)
 *     .collect { (response, contentFlow) ->
 *         println("Downloaded file with response: $response")
 *         contentFlow.collect { byteArray ->
 *             // Process byteArray
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
 * Creates a flow that uploads bytes to an Amazon S3 bucket.
 *
 * This function takes a [bucket], [key], and [upstream] flow of bytes and uploads them
 * to the specified S3 bucket. The function processes bytes in parallel using [parallelism].
 *
 * @param bucket The name of the S3 bucket.
 * @param key The key of the file to upload.
 * @param upstream A [Flow] of bytes to upload.
 * @param parallelism The level of parallelism for uploading bytes.
 * @return A [Flow] of [S3Response] objects.
 *
 * Example usage:
 * ```
 * val s3Client: S3AsyncClient = ...
 * val bucket = "my-bucket"
 * val key = "path/to/myfile.txt"
 * val byteFlow = flowOf<Byte> { ... } // A Flow<Byte> containing the bytes to upload.
 *
 * s3Client.uploadBytes(bucket, key, byteFlow)
 *     .collect { response ->
 *         println("Upload response: $response")
 *     }
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
 * Creates a flow that uploads byte arrays to an Amazon S3 bucket.
 *
 * This function takes a [bucket], [key], and [upstream] flow of byte arrays and uploads them
 * to the specified S3 bucket. The function processes byte arrays in parallel using [parallelism].
 *
 * @param bucket The name of the S3 bucket.
 * @param key The key of the file to upload.
 * @param upstream A [Flow] of byte arrays to upload.
 * @param parallelism The level of parallelism for uploading byte arrays.
 * @return A [Flow] of [S3Response] objects.
 *
 * Example usage:
 *
 * ```
 * val s3Client: S3AsyncClient = ...
 * val bucket = "my-bucket"
 * val key = "path/to/myfile.txt"
 * val byteArrayFlow = flowOf<ByteArray> { ... } // A Flow<ByteArray> containing the byte arrays to upload.
 *
 * s3Client.upload(bucket, key, byteArrayFlow)
 *     .collect { response ->
 *         println("Upload response: $response")
 *     }
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
