package com.river.connector.aws.s3

import aws.sdk.kotlin.services.s3.*
import aws.sdk.kotlin.services.s3.model.*
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.content.toFlow
import com.river.connector.aws.s3.model.S3Response
import com.river.core.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.yield

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
 * val s3Client:  S3Client = ...
 * val bucket = "my-bucket"
 * val key = "path/to/myfile.txt"
 *
 * s3Client
 *     .download(bucket, key)
 *     .collect { (response, contentFlow) ->
 *         println("Downloaded file with response: $response")
 *         contentFlow.collect { byteArray ->
 *             // Process byteArray
 *         }
 *     }
 * ```
 */
fun S3Client.download(
    bucket: String,
    key: String
): Flow<Pair<GetObjectResponse, Flow<ByteArray>>> =
    download {
        this.bucket = bucket
        this.key = key
    }


/**
 * Creates a flow that downloads a file from an Amazon S3 bucket.
 *
 * This function takes a builder function to create a [GetObjectRequest] and returns a [Flow] of pairs containing the
 * [GetObjectResponse] and a flow of byte arrays.
 *
 * @param builder The request builder function.
 *
 * @return A [Flow] of pairs containing the [GetObjectResponse] and a flow of byte arrays.
 *
 * Example usage:
 *
 * ```
 * val s3Client:  S3Client = ...
 * val bucket = "my-bucket"
 * val key = "path/to/myfile.txt"
 *
 * s3Client
 *     .download {
 *         bucket = "my-bucket"
 *         key = "path/to/myfile.txt"
 *     }
 *     .collect { (response, contentFlow) ->
 *         println("Got file with response: $response")
 *         println("Downloading...")
 *
 *         contentFlow.collect { byteArray ->
 *             // Process byteArray
 *         }
 *     }
 * ```
 */
fun S3Client.download(
    builder: GetObjectRequest.Builder.() -> Unit
): Flow<Pair<GetObjectResponse, Flow<ByteArray>>> =
    channelFlow {
        val channel = Channel<ByteArray>()

        getObject(GetObjectRequest { builder() }) { response ->
            (response.body?.toFlow() ?: emptyFlow())
                .onStart { send(response to channel.consumeAsFlow()) }
                .onCompletion { channel.close(it) }
                .collect { channel.send(it)}
        }

        while (!channel.isClosedForSend) {
            yield()
        }
    }

/**
 * A function that performs a select object content operation on a file from an Amazon S3 bucket, which allows
 * retrieving a subset of data from an object by using simple SQL expressions.
 *
 * The function uses [ S3Client] and processes the results as a flow.
 *
 * @param request A lambda function with [SelectObjectContentRequest.Builder] receiver to configure the request.
 * @return Returns a flow of [SelectObjectContentEventStream] representing the content of the selected object.
 *
 * Example usage:
 *
 * ```
 * val client =  S3Client.create()
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
fun S3Client.selectObjectContent(
    request: SelectObjectContentRequest.Builder.() -> Unit
): Flow<SelectObjectContentEventStream> =
    flow {
        val selectObjectRequest = SelectObjectContentRequest { request() }

        selectObjectContent(selectObjectRequest) { response ->
            response.payload?.also { emitAll(it) }
        }
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
 * val s3Client:  S3Client = ...
 * val byteFlow = flowOf<Byte> { ... } // A Flow<Byte> containing the bytes to upload.
 *
 * s3Client
 *     .uploadBytes(byteFlow) {
 *         bucket = "my-bucket"
 *         key = "path/to/myfile.txt"
 *     }
 *     .collect { response ->
 *         println("Upload response: $response")
 *     }
 * ```
 */
fun S3Client.uploadBytes(
    upstream: Flow<Byte>,
    concurrency: Int = 1,
    initialRequest: CreateMultipartUploadRequest.Builder.() -> Unit
): Flow<S3Response.MultipartResponse<*>> = flow {
    val uploadResponse = createMultipartUploadResponse(initialRequest)

    val eTags =
        upstream
            .chunked(MINIMUM_UPLOAD_SIZE)
            .withIndex()
            .mapAsync(concurrency) { (index, chunk) ->
                uploadPart {
                    bucket = uploadResponse.bucket
                    key = uploadResponse.key
                    uploadId = uploadResponse.uploadId
                    partNumber = index + 1
                    body = ByteStream.fromBytes(chunk.toByteArray())
                }
            }
            .onEach { emit(S3Response.UploadPart(it)) }
            .mapNotNull { it.eTag }
            .toList()

    completeMultipartUpload(uploadResponse, eTags)
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
 * val s3Client:  S3Client = ...
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
fun S3Client.uploadBytes(
    bucket: String,
    key: String,
    upstream: Flow<Byte>,
    concurrency: Int = 1,
): Flow<S3Response.MultipartResponse<*>> =
    uploadBytes(upstream, concurrency) {
        this.bucket = bucket
        this.key = key
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
 * val s3Client:  S3Client = ...
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
fun S3Client.upload(
    bucket: String,
    key: String,
    upstream: Flow<ByteArray>,
    concurrency: Int = 1
): Flow<S3Response.MultipartResponse<*>> =
    upload(
        upstream = upstream,
        concurrency = concurrency
    ) {
        this.bucket = bucket
        this.key = key
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
 * val s3Client:  S3Client = ...
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
fun S3Client.upload(
    upstream: Flow<ByteArray>,
    concurrency: Int = 1,
    initialRequest: CreateMultipartUploadRequest.Builder.() -> Unit
): Flow<S3Response.MultipartResponse<*>> =
    uploadBytes(
        upstream = upstream.flatMapIterable { it.toList() },
        concurrency = concurrency,
        initialRequest = initialRequest
    )

/**
 * This function uploads a file in chunks to an Amazon S3 bucket using the [ S3Client].
 *
 * Particularly useful for handling streams of unknown size, since it automatically splits the flow into separate files,
 * allowing for seamless processing and storage.
 *
 * When the split size exceeds 5MB, the function automatically utilizes S3's multipart file upload to prevent retaining
 * large chunks of data in-memory. If it is smaller, the function uses the put object operation for a quicker and more
 * efficient processing.
 *
 * @param bucket The S3 bucket to upload the file to.
 * @param upstream A flow of bytes representing the file to be uploaded.
 * @param splitStrategy The strategy to describe the split to be uploaded, in bytes. Default is 1 MB.
 * @param concurrency The number of concurrent uploads to use. Default is 1.
 * @param key A function that takes an integer (part number) and returns the key of the object in the S3 bucket.
 * @return A flow of S3Response objects for each uploaded chunk.
 *
 * Example usage:
 *
 * ```
 *  val s3Client:  S3Client = ...
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
fun S3Client.uploadSplit(
    bucket: String,
    upstream: Flow<Byte>,
    splitStrategy: GroupStrategy = GroupStrategy.Count(ONE_MB),
    concurrency: Int = 1,
    key: (Int) -> String
): Flow<S3Response<*>> =
    upstream
        .split(splitStrategy)
        .withIndex()
        .flatMapFlow { (part, chunk) ->
            internalUploadSplit(splitStrategy, chunk, bucket, key, part, concurrency)
        }

/**
 * This function uploads a file in chunks to an Amazon S3 bucket using the [ S3Client].
 *
 * Particularly useful for handling streams of unknown size, since it automatically splits the flow into separate files,
 * allowing for seamless processing and storage.
 *
 * When the split size exceeds 5MB, the function automatically utilizes S3's multipart file upload to prevent retaining
 * large chunks of data in-memory. If it is smaller, the function uses the put object operation for a quicker and more
 * efficient processing.
 *
 * @param bucket The S3 bucket to upload the file to.
 * @param upstream A flow of bytes representing the file to be uploaded.
 * @param splitStrategy The strategy to describe the split to be uploaded. Default is 1000 items.
 * @param concurrency The number of concurrent uploads to use. Default is 1.
 * @param key A function that takes an integer (part number) and returns the key of the object in the S3 bucket.
 * @param f A function that takes an object and converts it to an array of bytes.
 *
 * @return A flow of S3Response objects for each uploaded chunk.
 *
 * Example usage:
 *
 * ```
 *  val s3Client:  S3Client = ...
 *  val bucket = "my-bucket"
 *  val usersFlow = flowOf<User> { ... }
 *
 *  s3Client
 *      .uploadSplitItems(
 *          bucket = bucket,
 *          upstream = usersFlow,
 *          splitEach = TimeWindow(1000, 1.seconds),
 *          key = { part -> "folder/file-part-$part" }
 *      ) { user ->
 *          // convert the user into an array of bytes
 *      }
 *      .collect { response ->
 *          println("Uploaded part: ${response.key}")
 *      }
 * ```
 */
fun <T> S3Client.uploadSplitItems(
    bucket: String,
    upstream: Flow<T>,
    splitStrategy: GroupStrategy = GroupStrategy.Count(1000),
    concurrency: Int = 1,
    key: (Int) -> String,
    f: suspend (T) -> ByteArray
): Flow<S3Response<*>> =
    upstream
        .split(splitStrategy)
        .map { it.map(f).flatMapIterable { it.toList() } }
        .withIndex()
        .flatMapFlow { (part, chunk) ->
            internalUploadSplit(splitStrategy, chunk, bucket, key, part, concurrency)
        }

/**
 * This function performs a merge by using multipart upload copy operations using the [ S3Client].
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
 *  val s3Client =  S3Client.create()
 *
 *  val sourceFiles = flowOf(
 *      "source-bucket-1" to "file1.txt",
 *      "source-bucket-2" to "file2.txt",
 *      "source-bucket-3" to "file3.txt"
 *  )
 *
 *  s3Client
 *      .mergeContents(sourceFiles) {
 *          bucket = "my-bucket"
 *          key = "merged-file.txt"
 *      }
 *      .collect { response ->
 *          println("Copied part: ${response.key}")
 *      }
 * ```
 */
fun S3Client.mergeContents(
    files: List<Pair<String, String>>,
    concurrency: Int = 1,
    destination: CreateMultipartUploadRequest.Builder.() -> Unit
): Flow<S3Response<*>> = flow {
    val uploadResponse = createMultipartUploadResponse(destination)

    val eTags =
        files
            .withIndex()
            .mapAsync(concurrency) { (index, tuple) ->
                val (sourceBucket, sourceKey) = tuple

                uploadPartCopy {
                    copySource = "$sourceBucket/$sourceKey"
                    key = uploadResponse.key
                    bucket = uploadResponse.bucket
                    uploadId = uploadResponse.uploadId
                    partNumber = index + 1
                }
            }
            .onEach { emit(S3Response.UploadPartCopy(it)) }
            .mapNotNull { it.copyPartResult?.eTag }

    completeMultipartUpload(
        uploadResponse = uploadResponse,
        eTags = eTags
    )
}

private suspend fun S3Client.internalUploadSplit(
    splitStrategy: GroupStrategy,
    chunk: Flow<Byte>,
    bucket: String,
    key: (Int) -> String,
    part: Int,
    concurrency: Int
): Flow<S3Response<*>> {
    val size = when (splitStrategy) {
        is GroupStrategy.Count -> splitStrategy.size
        is GroupStrategy.TimeWindow -> splitStrategy.size
    }

    return if (size <= FIVE_MB) {
        flow {
            val eagerChunk = chunk.toList()

            val response = putObject {
                this.bucket = bucket
                this.key = key(part + 1)
                contentLength = eagerChunk.size.toLong()
                body = ByteStream.fromBytes(eagerChunk.toByteArray())
            }

            emit(S3Response.PutObject(response))
        }
    } else {
        uploadBytes(
            bucket = bucket,
            key = key(part + 1),
            upstream = chunk,
            concurrency = concurrency
        )
    }
}

context(S3Client, FlowCollector<S3Response.MultipartResponse<*>>)
private suspend fun createMultipartUploadResponse(
    initialRequest: CreateMultipartUploadRequest.Builder.() -> Unit
): CreateMultipartUploadResponse {
    val uploadResponse = createMultipartUpload { initialRequest() }
    emit(S3Response.CreateMultipartUpload(uploadResponse))
    return uploadResponse
}

context(S3Client, FlowCollector<S3Response.MultipartResponse<*>>)
private suspend inline fun completeMultipartUpload(
    uploadResponse: CreateMultipartUploadResponse,
    eTags: List<String>
) {
    val response = completeMultipartUpload {
        bucket = uploadResponse.bucket
        key = uploadResponse.key
        uploadId = uploadResponse.uploadId

        multipartUpload {
            parts = eTags.mapIndexed { index, e ->
                CompletedPart {
                    partNumber = index + 1
                    eTag = e
                }
            }
        }
    }

    emit(S3Response.CompleteMultipartUpload(response))
}
