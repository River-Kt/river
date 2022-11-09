package io.github.gabfssilva.river.aws.sqs

sealed class Acknowledgment {
    object Delete : Acknowledgment()

    object Ignore : Acknowledgment()

    class ChangeMessageVisibility(
        val timeout: Int
    ) : Acknowledgment()

    override fun toString() = this.javaClass.simpleName
}
