package io.github.gabfssilva.river.aws.sqs

import software.amazon.awssdk.core.SdkResponse
import software.amazon.awssdk.services.sqs.model.Message

data class AcknowledgmentResult<T : SdkResponse>(
    val message: Message,
    val acknowledgment: Acknowledgment,
    val response: SdkResponse?
)
