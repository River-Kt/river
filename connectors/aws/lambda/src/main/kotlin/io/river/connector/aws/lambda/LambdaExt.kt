package io.river.connector.aws.lambda

import io.river.core.mapParallel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.future.await
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.lambda.LambdaAsyncClient
import software.amazon.awssdk.services.lambda.model.InvocationType
import software.amazon.awssdk.services.lambda.model.InvokeResponse
import software.amazon.awssdk.services.lambda.model.LogType

context(Flow<String>)
fun LambdaAsyncClient.invokeFlow(
    functionName: String,
    invocationType: InvocationType = InvocationType.REQUEST_RESPONSE,
    logType: LogType? = null,
    qualifier: String? = null,
    clientContext: String? = null,
    parallelism: Int = 1
): Flow<InvokeResponse> =
    mapParallel(parallelism) { content ->
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

fun LambdaAsyncClient.invokeFlow(
    upstream: Flow<String>,
    functionName: String,
    invocationType: InvocationType = InvocationType.REQUEST_RESPONSE,
    logType: LogType? = null,
    qualifier: String? = null,
    clientContext: String? = null,
    parallelism: Int = 1
): Flow<InvokeResponse> =
    with(upstream) {
        invokeFlow(
            functionName = functionName,
            invocationType = invocationType,
            logType = logType,
            qualifier = qualifier,
            clientContext = clientContext,
            parallelism = parallelism
        )
    }
