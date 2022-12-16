package io.river.connector.jms.model

enum class SessionMode(val value: Int) {
    SESSION_TRANSACTED(0),
    AUTO_ACKNOWLEDGE(1),
    CLIENT_ACKNOWLEDGE(2),
    DUPS_OK_ACKNOWLEDGE(3)
}
