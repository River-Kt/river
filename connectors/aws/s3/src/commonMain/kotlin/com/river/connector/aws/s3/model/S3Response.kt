package com.river.connector.aws.s3.model

import aws.sdk.kotlin.services.s3.model.*

sealed interface S3Response<T> {
    val bucketKeyEnabled: Boolean?
    val ssekmsKeyId: String?
    val serverSideEncryption: ServerSideEncryption?
    val requestCharged: RequestCharged?

    val response: T

    sealed interface MultipartResponse<T> : S3Response<T>

    data class CreateMultipartUpload(
        override val response: CreateMultipartUploadResponse
    ) : MultipartResponse<CreateMultipartUploadResponse> {
        override val bucketKeyEnabled: Boolean? = response.bucketKeyEnabled
        override val ssekmsKeyId: String? = response.ssekmsKeyId
        override val serverSideEncryption: ServerSideEncryption? = response.serverSideEncryption
        override val requestCharged: RequestCharged? = response.requestCharged
    }

    data class UploadPart(
        override val response: UploadPartResponse
    ) : MultipartResponse<UploadPartResponse> {
        override val bucketKeyEnabled: Boolean? = response.bucketKeyEnabled
        override val ssekmsKeyId: String? = response.ssekmsKeyId
        override val serverSideEncryption: ServerSideEncryption? = response.serverSideEncryption
        override val requestCharged: RequestCharged? = response.requestCharged
    }

    data class CompleteMultipartUpload(
        override val response: CompleteMultipartUploadResponse
    ) : MultipartResponse<CompleteMultipartUploadResponse> {
        override val bucketKeyEnabled: Boolean? = response.bucketKeyEnabled
        override val ssekmsKeyId: String? = response.ssekmsKeyId
        override val serverSideEncryption: ServerSideEncryption? = response.serverSideEncryption
        override val requestCharged: RequestCharged? = response.requestCharged
    }

    data class UploadPartCopy(
        override val response: UploadPartCopyResponse
    ) : MultipartResponse<UploadPartCopyResponse> {
        override val bucketKeyEnabled: Boolean? = response.bucketKeyEnabled
        override val ssekmsKeyId: String? = response.ssekmsKeyId
        override val serverSideEncryption: ServerSideEncryption? = response.serverSideEncryption
        override val requestCharged: RequestCharged? = response.requestCharged
    }

    data class PutObject(override val response: PutObjectResponse) : S3Response<PutObjectResponse> {
        override val bucketKeyEnabled: Boolean? = response.bucketKeyEnabled
        override val ssekmsKeyId: String? = response.ssekmsKeyId
        override val serverSideEncryption: ServerSideEncryption? = response.serverSideEncryption
        override val requestCharged: RequestCharged? = response.requestCharged
    }
}
