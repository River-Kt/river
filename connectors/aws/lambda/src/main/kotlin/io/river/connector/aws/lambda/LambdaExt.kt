package io.river.connector.aws.lambda

import io.river.core.mapParallel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.future.await
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.lambda.LambdaAsyncClient
import software.amazon.awssdk.services.lambda.model.InvocationType
import software.amazon.awssdk.services.lambda.model.InvokeResponse
import software.amazon.awssdk.services.lambda.model.LogType

fun LambdaAsyncClient.invokeFlow(
    functionName: String,
    upstream: Flow<String>,
    invocationType: InvocationType = InvocationType.REQUEST_RESPONSE,
    logType: LogType? = null,
    qualifier: String? = null,
    clientContext: String? = null,
    parallelism: Int = 1
): Flow<InvokeResponse> =
    upstream
        .mapParallel(parallelism) { content ->
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
