package io.river.connector.aws.sqs

import io.river.core.mapParallel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.future.await
import software.amazon.awssdk.services.sesv2.SesV2AsyncClient
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest

fun <T> Flow<T>.asSendEmailRequest(
    f: SendEmailRequest.Builder.(T) -> Unit
): Flow<SendEmailRequest> =
    map { SendEmailRequest.builder().also { b -> f(b, it) }.build() }

fun SesV2AsyncClient.sendEmailFlow(
    upstream: Flow<SendEmailRequest>,
    parallelism: Int = 1
) = with(upstream) { sendEmailFlow(parallelism) }

context(Flow<SendEmailRequest>)
fun SesV2AsyncClient.sendEmailFlow(
    parallelism: Int = 1
) = mapParallel(parallelism) { sendEmail(it).await() }
