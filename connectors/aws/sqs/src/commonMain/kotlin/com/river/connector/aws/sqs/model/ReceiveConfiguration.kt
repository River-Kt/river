package com.river.connector.aws.sqs.model

import aws.sdk.kotlin.services.sqs.model.ReceiveMessageRequest
import com.river.core.GroupStrategy
import kotlin.time.Duration.Companion.milliseconds

class ReceiveConfiguration {
    var stopOnEmptyList: Boolean = false
    var concurrency = 1

    internal var request: ReceiveMessageRequest.Builder.() -> Unit = {}

    fun receiveRequest(f: ReceiveMessageRequest.Builder.() -> Unit) {
        request = f
    }
}

class CommitConfiguration {
    var concurrency = 1
    var groupStrategy = GroupStrategy.TimeWindow(10, 250.milliseconds)
}
