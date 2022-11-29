package io.github.gabfssilva.river.jms

import javax.jms.*

sealed class CommitableMessage(
    private val coAcknowledge: suspend () -> Unit,
) : Message {
    suspend fun coAcknowledge() = coAcknowledge.invoke()

    class DefaultMessage(
        inner: Message,
        coAcknowledge: suspend () -> Unit
    ) : CommitableMessage(coAcknowledge), Message by inner {
        override fun acknowledge(): Unit =
            error("You must not call acknowledge directly. Please, call coAcknowledge.")
    }

    class CommitableMapMessage(
        inner: MapMessage,
        coAcknowledge: suspend () -> Unit
    ) : CommitableMessage(coAcknowledge), MapMessage by inner {
        override fun acknowledge(): Unit =
            error("You must not call acknowledge directly. Please, call coAcknowledge.")
    }

    class CommitableObjectMessage(
        inner: ObjectMessage,
        coAcknowledge: suspend () -> Unit
    ) : CommitableMessage(coAcknowledge), ObjectMessage by inner {
        override fun acknowledge(): Unit {
            error("You must not call acknowledge directly. Please, call coAcknowledge.")
        }
    }

    class CommitableTextMessage(
        inner: TextMessage,
        coAcknowledge: suspend () -> Unit
    ) : CommitableMessage(coAcknowledge), TextMessage by inner {
        override fun acknowledge(): Unit =
            error("You must not call acknowledge directly. Please, call coAcknowledge.")
    }

    class CommitableBytesMessage(
        inner: BytesMessage,
        coAcknowledge: suspend () -> Unit
    ) : CommitableMessage(coAcknowledge), BytesMessage by inner {
        override fun acknowledge(): Unit =
            error("You must not call acknowledge directly. Please, call coAcknowledge.")
    }

    companion object {
        operator fun invoke(
            message: Message,
            ack: suspend () -> Unit
        ) = when (message) {
            is MapMessage -> CommitableMapMessage(message, ack)
            is TextMessage -> CommitableTextMessage(message, ack)
            is ObjectMessage -> CommitableObjectMessage(message, ack)
            is BytesMessage -> CommitableBytesMessage(message, ack)
            else -> DefaultMessage(message, ack)
        }
    }
}
