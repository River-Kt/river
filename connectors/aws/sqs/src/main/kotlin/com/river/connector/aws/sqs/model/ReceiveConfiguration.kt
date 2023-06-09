package com.river.connector.aws.sqs.model

import com.river.core.GroupStrategy
import kotlin.time.Duration.Companion.milliseconds

class ReceiveConfiguration {
    var stopOnEmptyList: Boolean = false
    var concurrency = 1

    internal var request: ReceiveMessageRequestBuilder.() -> Unit = {}

    fun receiveRequest(f: ReceiveMessageRequestBuilder.() -> Unit) {
        request = f
    }
}

class CommitConfiguration {
    var concurrency = 1
    var groupStrategy = GroupStrategy.TimeWindow(10, 250.milliseconds)
}
