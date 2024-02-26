package com.river.connector.aws.sqs.model

import kotlin.time.Duration

sealed interface OnError {
    data object Throw : OnError
    data object Complete : OnError

    class Retry(
        val delay: Duration,
        val maxAttempts: Long = Long.MAX_VALUE
    )  : OnError
}
