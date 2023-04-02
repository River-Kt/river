package com.river.connector.jms.model

import javax.jms.*

sealed class CommittableMessage(
    private val coAcknowledge: suspend () -> Unit,
) : Message {
    suspend fun coAcknowledge() = coAcknowledge.invoke()

    class DefaultMessage(
        inner: Message,
        coAcknowledge: suspend () -> Unit
    ) : CommittableMessage(coAcknowledge), Message by inner {
        override fun acknowledge(): Unit =
            error("You must not call acknowledge directly. Please, call coAcknowledge.")
    }

    class CommittableMapMessage(
        inner: MapMessage,
        coAcknowledge: suspend () -> Unit
    ) : CommittableMessage(coAcknowledge), MapMessage by inner {
        override fun acknowledge(): Unit =
            error("You must not call acknowledge directly. Please, call coAcknowledge.")
    }

    class CommittableObjectMessage(
        inner: ObjectMessage,
        coAcknowledge: suspend () -> Unit
    ) : CommittableMessage(coAcknowledge), ObjectMessage by inner {
        override fun acknowledge(): Unit {
            error("You must not call acknowledge directly. Please, call coAcknowledge.")
        }
    }

    class CommittableTextMessage(
        inner: TextMessage,
        coAcknowledge: suspend () -> Unit
    ) : CommittableMessage(coAcknowledge), TextMessage by inner {
        override fun acknowledge(): Unit =
            error("You must not call acknowledge directly. Please, call coAcknowledge.")
    }

    class CommittableBytesMessage(
        inner: BytesMessage,
        coAcknowledge: suspend () -> Unit
    ) : CommittableMessage(coAcknowledge), BytesMessage by inner {
        override fun acknowledge(): Unit =
            error("You must not call acknowledge directly. Please, call coAcknowledge.")
    }

    companion object {
        operator fun invoke(
            message: Message,
            ack: suspend () -> Unit
        ) = when (message) {
            is MapMessage -> CommittableMapMessage(message, ack)
            is TextMessage -> CommittableTextMessage(message, ack)
            is ObjectMessage -> CommittableObjectMessage(message, ack)
            is BytesMessage -> CommittableBytesMessage(message, ack)
            else -> DefaultMessage(message, ack)
        }
    }
}
