@file:OptIn(FlowPreview::class)

package io.github.gabfssilva.river.aws.s3

import io.github.gabfssilva.river.core.chunked
import io.github.gabfssilva.river.core.mapParallel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.future.await
import kotlinx.coroutines.reactive.asFlow
import software.amazon.awssdk.core.async.AsyncRequestBody.fromBytes
import software.amazon.awssdk.core.async.AsyncResponseTransformer
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.CompletedPart
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import software.amazon.awssdk.services.s3.model.S3Response
import java.util.*

private const val MINIMUM_UPLOAD_SIZE = 1024 * 1024 * 5

suspend fun S3AsyncClient.download(
    bucket: String,
    key: String
): Pair<GetObjectResponse, Flow<ByteArray>> =
    getObject({ it.bucket(bucket).key(key) }, AsyncResponseTransformer.toPublisher())
        .await()
        .let { responsePublisher ->
            responsePublisher.response() to responsePublisher.asFlow().map { it.array() }
        }

fun S3AsyncClient.upload(
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

object bytes {
    fun S3AsyncClient.upload(
        bucket: String,
        key: String,
        upstream: Flow<ByteArray>,
        parallelism: Int = 1
    ): Flow<S3Response> = upload(
        bucket = bucket,
        key = key,
        upstream = upstream.flatMapConcat { it.toList().asFlow() },
        parallelism = parallelism
    )
}

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
        .multipartUpload {
            val completed =
                etags.mapIndexed { number, part ->
                    CompletedPart
                        .builder()
                        .apply { partNumber(number + 1).eTag(part) }
                        .build()
                }

            it.parts(completed)
        }
}.await()
