@file:OptIn(FlowPreview::class)

package io.github.gabfssilva.river.aws.s3

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.future.await
import kotlinx.coroutines.reactive.asFlow
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.core.async.AsyncResponseTransformer
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse
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
        .let { it.response() to it.asFlow().map { it.array() } }

fun S3AsyncClient.upload(
    bucket: String,
    key: String,
    content: Flow<ByteArray>
): Flow<S3Response> = flow {
    val uploadResponse = createMultipartUpload { it.bucket(bucket).key(key) }.await()
    emit(uploadResponse)
    val uploadId = uploadResponse.uploadId()

    var currentPart = mutableListOf<Byte>()
    val uploadedParts = mutableListOf<String>()

    suspend fun upload() {
        if (currentPart.isEmpty()) return

        uploadPart(
            {
                it.bucket(bucket)
                    .key(key)
                    .uploadId(uploadId)
                    .partNumber(uploadedParts.size + 1)
            }, AsyncRequestBody.fromBytes(currentPart.toByteArray())
        ).await().also { emit(it) }.let { uploadedParts.add(it.eTag()) }

        currentPart = mutableListOf()
    }

    suspend fun complete(): CompleteMultipartUploadResponse =
        completeMultipartUpload { builder ->
            builder
                .bucket(bucket)
                .key(key)
                .uploadId(uploadId)
                .multipartUpload {
                    val completed =
                        uploadedParts
                            .mapIndexed { number, part ->
                                CompletedPart
                                    .builder()
                                    .apply { partNumber(number + 1).eTag(part) }
                                    .build()
                            }

                    it.parts(completed)
                }
        }.await()


    content
        .onCompletion {
            if (it == null) {
                upload()
                emit(complete())
            }
        }
        .flatMapConcat { it.asList().asFlow() }
        .fold(0) { sentBytes, byte ->
            val newSentBytes = sentBytes + 1
            currentPart.add(byte)
            if (newSentBytes >= MINIMUM_UPLOAD_SIZE) upload().let { 0 }
            else newSentBytes
        }
}
