package com.river.connector.aws.lambda

import aws.sdk.kotlin.services.lambda.LambdaClient
import aws.sdk.kotlin.services.lambda.invoke
import aws.sdk.kotlin.services.lambda.model.InvokeRequest
import aws.sdk.kotlin.services.lambda.model.InvokeResponse
import com.river.core.mapAsync
import kotlinx.coroutines.flow.Flow

/**
 * Creates a flow that invokes an AWS Lambda function with the specified [builder] request.
 *
 * This function takes an [upstream] flow of input byte arrays and invokes the specified Lambda
 * function with the given parameters concurrently based on the provided [concurrency].
 *
 * @param upstream A ByteArray based [Flow] of input payloads.
 * @param concurrency The level of concurrency for invoking the Lambda function.
 * @param builder The InvokeRequest's builder
 * @return A [Flow] of [InvokeResponse] objects.
 *
 * Example usage:
 *
 * ```
 * val lambdaClient: LambdaAsyncClient = ...
 * val functionName =
 * val inputs = flowOf("input1", "input2", "input3")
 *
 * lambdaClient
 *     .invokeFlow(inputs, concurrency = 10) { item ->
 *         functionName = "my-lambda-function"
 *         payload = item.encodeToByteArray()
 *     }
 *     .collect { response ->
 *         println("Lambda invocation response: $response")
 *     }
 * ```
 */
inline fun <T> LambdaClient.invokeFlow(
    upstream: Flow<T>,
    concurrency: Int = 1,
    crossinline builder: InvokeRequest.Builder.(T) -> Unit
): Flow<InvokeResponse> = upstream.mapAsync(concurrency) { content -> invoke { builder(content) } }
