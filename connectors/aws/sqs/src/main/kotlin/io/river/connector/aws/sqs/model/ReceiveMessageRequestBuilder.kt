package io.river.connector.aws.sqs.model

import software.amazon.awssdk.services.sqs.model.QueueAttributeName
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest

class ReceiveMessageRequestBuilder(
    var queueUrl: String? = null,
    var waitTimeSeconds: Int = 20,
    var maxNumberOfMessages: Int = 10,
    var visibilityTimeout: Int = 30,
    var receiveRequestAttemptId: String? = null,
    var messageAttributeNames: List<String> = emptyList(),
    var attributeNames: List<QueueAttributeName> = emptyList(),
) {
    fun build() =
        ReceiveMessageRequest
            .builder()
                .receiveRequestAttemptId(receiveRequestAttemptId)
                .messageAttributeNames(messageAttributeNames)
                .attributeNames(attributeNames)
                .waitTimeSeconds(waitTimeSeconds)
                .maxNumberOfMessages(maxNumberOfMessages)
                .visibilityTimeout(visibilityTimeout)
                .queueUrl(queueUrl)
            .build()
}
