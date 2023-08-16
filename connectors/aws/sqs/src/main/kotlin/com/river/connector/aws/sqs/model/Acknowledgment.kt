package com.river.connector.aws.sqs.model

sealed class Acknowledgment {
    object Delete : Acknowledgment()

    object Ignore : Acknowledgment()

    class ChangeMessageVisibility(
        val timeout: Int
    ) : Acknowledgment()

    override fun toString(): String = this.javaClass.simpleName
}
