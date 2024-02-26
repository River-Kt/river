package com.river.connector.aws.sqs.internal

import com.river.core.GroupStrategy

internal fun GroupStrategy.validate() {
    val chunkSize = when (this) {
        is GroupStrategy.Count -> size
        is GroupStrategy.TimeWindow -> size
    }

    require(chunkSize in 1..10) {
        "Due to SQS limitations, the chunk size must be between 1 and 10. Configured value: $chunkSize"
    }
}
