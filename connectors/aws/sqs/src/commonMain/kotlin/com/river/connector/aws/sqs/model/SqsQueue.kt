package com.river.connector.aws.sqs.model

import aws.sdk.kotlin.services.sqs.SqsClient
import aws.sdk.kotlin.services.sqs.getQueueUrl

fun interface SqsQueue {
    context(SqsClient)
    suspend fun url(): String

    companion object {
        fun name(name: String) = SqsQueue { getQueueUrl { queueName = name }.queueUrl.orEmpty() }
        fun url(url: String) = SqsQueue { url }
    }
}
