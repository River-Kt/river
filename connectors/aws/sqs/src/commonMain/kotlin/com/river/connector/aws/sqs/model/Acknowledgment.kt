package com.river.connector.aws.sqs.model

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

sealed class Acknowledgment {
    object Delete : Acknowledgment()

    object Ignore : Acknowledgment()

    class ChangeMessageVisibility(
        val timeout: Duration
    ) : Acknowledgment() {
        init {
            require(timeout <= 12.hours) { "Visibility timeout cannot be higher than 12 hours" }
        }

        val timeoutInSeconds = timeout.inWholeSeconds.toInt()
    }

    override fun toString(): String = this.javaClass.simpleName
}
