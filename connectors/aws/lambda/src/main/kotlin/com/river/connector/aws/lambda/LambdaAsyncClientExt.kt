package com.river.connector.aws.lambda

import com.river.core.mapAsync
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.future.await
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.lambda.LambdaAsyncClient
import software.amazon.awssdk.services.lambda.model.InvocationType
import software.amazon.awssdk.services.lambda.model.InvokeResponse
import software.amazon.awssdk.services.lambda.model.LogType

/**
 * Creates a flow that invokes an AWS Lambda function with the specified [functionName].
 *
 * This function takes an [upstream] flow of input strings and invokes the specified Lambda
 * function with the given parameters concurrently based on the provided [concurrency].
 *
 * @param functionName The name of the Lambda function.
 * @param upstream A [Flow] of input payloads.
 * @param invocationType The type of invocation (default: [InvocationType.REQUEST_RESPONSE]).
 * @param logType The log type (optional).
 * @param qualifier The function version or alias (optional).
 * @param clientContext The client context (optional).
 * @param concurrency The level of concurrency for invoking the Lambda function.
 * @return A [Flow] of [InvokeResponse] objects.
 *
 * Example usage:
 *
 * ```
 * val lambdaClient: LambdaAsyncClient = ...
 * val functionName = "my-lambda-function"
 * val inputs = flowOf("input1", "input2", "input3")
 *
 * lambdaClient.invokeFlow(functionName, inputs)
 *     .collect { response ->
 *         println("Lambda invocation response: $response")
 *     }
 * ```
 */
fun LambdaAsyncClient.invokeFlow(
    functionName: String,
    upstream: Flow<String>,
    invocationType: InvocationType = InvocationType.REQUEST_RESPONSE,
    logType: LogType? = null,
    qualifier: String? = null,
    clientContext: String? = null,
    concurrency: Int = 1
): Flow<InvokeResponse> =
    upstream
        .mapAsync(concurrency) { content ->
            invoke { builder ->
                builder
                    .functionName(functionName)
                    .invocationType(invocationType)
                    .logType(logType)
                    .qualifier(qualifier)
                    .clientContext(clientContext)
                    .payload(SdkBytes.fromUtf8String(content))
            }.await()
        }
