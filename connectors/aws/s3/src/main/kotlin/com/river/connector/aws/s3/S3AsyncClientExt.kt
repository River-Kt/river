@file:OptIn(ExperimentalCoroutinesApi::class)

package com.river.connector.aws.s3

import com.river.core.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.future.await
import kotlinx.coroutines.reactive.asFlow
import software.amazon.awssdk.core.async.AsyncRequestBody.fromBytes
import software.amazon.awssdk.core.async.AsyncResponseTransformer
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.*
import software.amazon.awssdk.services.s3.model.CompletedPart.builder

private const val ONE_KB = 1024
private const val ONE_MB = ONE_KB * ONE_KB
private const val FIVE_MB = ONE_MB * 5
private const val MINIMUM_UPLOAD_SIZE = FIVE_MB

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
    flowOfSuspend {
        getObject({ it.bucket(bucket).key(key) }, AsyncResponseTransformer.toPublisher())
            .await()
            .let { it.response() to it.asFlow().asByteArray() }
    }

/**
 * A function that performs a select object content operation on a file from an Amazon S3 bucket, which allows
 * retrieving a subset of data from an object by using simple SQL expressions.
 *
 * The function uses [S3AsyncClient] and processes the results as a flow.
 *
 * @param request A lambda function with [SelectObjectContentRequest.Builder] receiver to configure the request.
 * @return Returns a flow of [SelectObjectContentEventStream] representing the content of the selected object.
 *
 * Example usage:
 *
 * ```
 * val client = S3AsyncClient.create()
 *
 * val selectObjectContentFlow = client.selectObjectContent {
 *     bucket("people-bucket")
 *     key("people-data.csv")
 *     expression("SELECT * FROM S3Object s WHERE s.age > 25")
 *     expressionType(ExpressionType.SQL)
 *     inputSerialization { serialization ->
 *         serialization.csv { it.fileHeaderInfo(FileHeaderInfo.USE) }
 *     }
 *     outputSerialization {
 *         csv(CsvOutputSerialization.builder().build())
 *     }
 * }
 *
 * selectObjectContentFlow
 *     .filterIsInstance<RecordsEvent>()
 *     .collect { event ->
 *         val record = String(event.payload().asUtf8String())
 *         // You may use the connector-format-csv module as well
 *         val (id, name, age) = record.split(",")
 *         println("Id: $id, Name: $name, Age: $age")
 *     }
 * }
 * ```
 */
fun S3AsyncClient.selectObjectContent(
    request: SelectObjectContentRequest.Builder.() -> Unit
): Flow<SelectObjectContentEventStream> =
    promiseFlow { promise ->
        val selectObjectRequest = SelectObjectContentRequest.builder().also(request).build()

        val responseHandler =
            SelectObjectContentResponseHandler
                .builder()
                    .onEventStream { promise.complete(it.asFlow()) }
                    .onError { promise.completeExceptionally(it) }
                .build()

        selectObjectContent(selectObjectRequest, responseHandler)
    }

/**
 * Creates a flow that uploads bytes to an Amazon S3 bucket.
 *
 * This function works under the assumption that the upstream size is unknown, so it always uses the multipart upload strategy,
 * as a versatile, "one-size-fits-all" solution.
 *
 * This function takes a [initialRequest] for the initial multipart upload request.
 * The function processes bytes concurrently using [concurrency].
 *
 * @param upstream A [Flow] of bytes to upload.
 * @param concurrency The level of concurrency for uploading bytes.
 * @param initialRequest A builder function for the initial multipart upload request.
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
    upstream: Flow<Byte>,
    concurrency: Int = 1,
    initialRequest: CreateMultipartUploadRequest.Builder.() -> Unit
): Flow<S3Response> = flow {
    val uploadResponse = createMultipartUpload { initialRequest(it) }.await()
    emit(uploadResponse)

    val bucket = uploadResponse.bucket()
    val key = uploadResponse.key()
    val uploadId = uploadResponse.uploadId()

    val uploadedParts =
        upstream
            .chunked(MINIMUM_UPLOAD_SIZE)
            .withIndex()
            .mapAsync(concurrency) { (part, chunk) -> uploadPart(bucket, key, uploadId, part, chunk) }
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
 * Creates a flow that uploads bytes to an Amazon S3 bucket.
 *
 * This function works under the assumption that the upstream size is unknown, so it always uses the multipart upload strategy,
 * as a versatile, "one-size-fits-all" solution.
 *
 * This function takes a [bucket], [key], and [upstream] flow of bytes and uploads them
 * to the specified S3 bucket. The function processes bytes concurrently using [concurrency].
 *
 * @param bucket The name of the S3 bucket.
 * @param key The key of the file to upload.
 * @param upstream A [Flow] of bytes to upload.
 * @param concurrency The level of concurrency for uploading bytes.
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
    concurrency: Int = 1,
): Flow<S3Response> =
    uploadBytes(upstream, concurrency) {
        bucket(bucket)
        key(key)
    }

/**
 * Creates a flow that uploads byte arrays to an Amazon S3 bucket.
 *
 * This function works under the assumption that the upstream size is unknown, so it always uses the multipart upload strategy,
 * as a versatile, "one-size-fits-all" solution.
 *
 * This function takes a [bucket], [key], and [upstream] flow of bytes and uploads them
 * to the specified S3 bucket. The function processes bytes concurrently using [concurrency].
 *
 * @param bucket The name of the S3 bucket.
 * @param key The key of the file to upload.
 * @param upstream A [Flow] of byte arrays to upload.
 * @param concurrency The level of concurrency for uploading byte arrays.
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
    concurrency: Int = 1
): Flow<S3Response> =
    upload(
        upstream = upstream,
        concurrency = concurrency
    ) {
        bucket(bucket)
        key(key)
    }

/**
 * Creates a flow that uploads byte arrays to an Amazon S3 bucket.
 *
 * This function works under the assumption that the upstream size is unknown, so it always uses the multipart upload strategy,
 * as a versatile, "one-size-fits-all" solution.
 *
 * This function takes a [initialRequest] for the initial multipart upload request.
 * The function processes bytes concurrently using [concurrency].
 *
 * @param upstream A [Flow] of byte arrays to upload.
 * @param concurrency The level of concurrency for uploading byte arrays.
 * @param initialRequest A builder function for the initial multipart upload request.
 *
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
 * s3Client
 *     .upload(byteArrayFlow) {
 *         bucket(bucket)
 *         key(key)
 *     }
 *     .collect { response ->
 *         println("Upload response: $response")
 *     }
 * ```
 */
fun S3AsyncClient.upload(
    upstream: Flow<ByteArray>,
    concurrency: Int = 1,
    initialRequest: CreateMultipartUploadRequest.Builder.() -> Unit
): Flow<S3Response> =
    uploadBytes(
        upstream = upstream.flatMapConcat { it.toList().asFlow() },
        concurrency = concurrency,
        initialRequest = initialRequest
    )

/**
 * This function uploads a file in chunks to an Amazon S3 bucket using the [S3AsyncClient].
 *
 * Particularly useful for handling streams of unknown size, since it automatically splits the flow into separate files,
 * allowing for seamless processing and storage.
 *
 * When [splitEach] exceeds 5MB, the function automatically utilizes S3's multipart file upload to prevent retaining
 * large chunks of data in-memory. If it is smaller, the function uses the put object operation for a quicker and more
 * efficient processing.
 *
 * @param bucket The S3 bucket to upload the file to.
 * @param upstream A flow of bytes representing the file to be uploaded.
 * @param splitEach The size of each chunk to be uploaded, in bytes. Default is 1 MB.
 * @param concurrency The number of concurrent uploads to use. Default is 1.
 * @param key A function that takes an integer (part number) and returns the key of the object in the S3 bucket.
 * @return A flow of S3Response objects for each uploaded chunk.
 *
 * Example usage:
 *
 * ```
 *  val s3Client: S3AsyncClient = ...
 *  val bucket = "my-bucket"
 *  val byteArrayFlow = flowOf<ByteArray> { ... } // A Flow<ByteArray> containing the byte arrays to upload.
 *  val oneMB = 1024 * 1024
 *
 *  s3Client.uploadSplit(bucket = bucket, upstream = byteArrayFlow, splitEach = oneMB) { part ->
 *      "folder/file-part-$part"
 *  }
 *  .collect { response ->
 *      println("Uploaded part: ${response.key}")
 *  }
 * ```
 */
fun S3AsyncClient.uploadSplit(
    bucket: String,
    upstream: Flow<Byte>,
    splitEach: Int = ONE_MB,
    concurrency: Int = 1,
    key: (Int) -> String
): Flow<S3Response> =
    upstream
        .split(splitEach)
        .withIndex()
        .flatMapConcat { (part, chunk) ->
            if (splitEach <= FIVE_MB) {
                val eagerChunk = chunk.toList()

                val request =
                    PutObjectRequest
                        .builder()
                        .bucket(bucket)
                        .key(key(part + 1))
                        .contentLength(eagerChunk.size.toLong())
                        .build()

                suspend { putObject(request, fromBytes(eagerChunk.toByteArray())).await() }.asFlow()
            } else {
                uploadBytes(
                    bucket = bucket,
                    key = key(part + 1),
                    upstream = chunk,
                    concurrency = concurrency
                )
            }
        }

/**
 * This function performs a merge by using multipart upload copy operations using the [S3AsyncClient].
 * It copies multiple source files into a single destination object in an S3 bucket.
 *
 * @param bucket The S3 bucket where the destination object will be stored.
 * @param key The key of the destination object in the S3 bucket.
 * @param concurrency The number of concurrent copy operations to perform. Default is 1.
 * @param files A flow of source file pairs, where the first element is the source bucket and the second element is the source key.
 *
 * @return A flow of S3Response objects for each part of the multipart copy operation.
 *
 * Example usage:
 * ```
 *  val s3Client = S3AsyncClient.create()
 *  val bucket = "my-bucket"
 *  val destinationKey = "merged-file.txt"
 *
 *  val sourceFiles = flowOf(
 *      "source-bucket-1" to "file1.txt",
 *      "source-bucket-2" to "file2.txt",
 *      "source-bucket-3" to "file3.txt"
 *  )
 *
 *  s3Client.multipartUploadCopy(
 *      bucket = bucket,
 *      key = destinationKey,
 *      concurrency = 2,
 *      files = sourceFiles
 *  ).collect { response ->
 *      println("Copied part: ${response.key}")
 *  }
 * ```
 */
fun S3AsyncClient.mergeContents(
    bucket: String,
    key: String,
    concurrency: Int = 1,
    files: List<Pair<String, String>>
): Flow<S3Response> = flow {
    val uploadResponse = createMultipartUpload { it.bucket(bucket).key(key) }.await()

    emit(uploadResponse)

    val etags =
        files
            .withIndex()
            .mapAsync(concurrency) { (index, tuple) ->
                val (sourceBucket, sourceKey) = tuple

                uploadPartCopy {
                    it.sourceBucket(sourceBucket)
                        .sourceKey(sourceKey)
                        .destinationBucket(bucket)
                        .destinationKey(key)
                        .uploadId(uploadResponse.uploadId())
                        .partNumber(index + 1)
                }.await()
            }
            .onEach { emit(it) }
            .map { it.copyPartResult().eTag() }

    completeMultipartUpload(bucket, key, uploadResponse.uploadId(), etags)
        .also { emit(it) }
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
        .multipartUpload { complete ->
            etags
                .mapIndexed { number, part ->
                    builder().apply { partNumber(number + 1).eTag(part) }.build()
                }
                .let { complete.parts(it) }
        }
}.await()
