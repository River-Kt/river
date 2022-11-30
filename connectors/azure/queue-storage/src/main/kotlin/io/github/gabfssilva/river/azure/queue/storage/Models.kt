package io.github.gabfssilva.river.azure.queue.storage

import kotlin.time.Duration

data class SendMessageRequest(
    val text: String,
    val visibilityTimeout: Duration? = null,
    val ttl: Duration? = null,
)
