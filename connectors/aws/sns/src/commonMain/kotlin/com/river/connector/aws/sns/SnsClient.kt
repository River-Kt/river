package com.river.connector.aws.sns

import com.river.connector.aws.sns.model.PublishMessageRequest
import com.river.connector.aws.sns.model.PublishMessageResponse

expect interface SnsClient {
    suspend fun publishBatch(
        topicArn: String,
        messages: List<PublishMessageRequest>,
    ): List<PublishMessageResponse>
}
