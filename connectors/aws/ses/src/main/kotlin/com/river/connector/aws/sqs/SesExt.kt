package com.river.connector.aws.sqs

import com.river.core.mapAsync
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.future.await
import software.amazon.awssdk.services.sesv2.SesV2AsyncClient
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest
import software.amazon.awssdk.services.sesv2.model.SendEmailResponse

fun <T> Flow<T>.asSendEmailRequest(
    f: SendEmailRequest.Builder.(T) -> Unit
): Flow<SendEmailRequest> =
    map { SendEmailRequest.builder().also { b -> f(b, it) }.build() }

fun SesV2AsyncClient.sendEmailFlow(
    upstream: Flow<SendEmailRequest>,
    parallelism: Int = 1
): Flow<SendEmailResponse> =
    upstream
        .mapAsync(parallelism) {
            sendEmail(it).await()
        }
