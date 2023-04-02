package com.river.connector.azure.queue.storage.model

import kotlin.time.Duration

data class SendMessageRequest(
    val text: String,
    val visibilityTimeout: Duration? = null,
    val ttl: Duration? = null,
)
