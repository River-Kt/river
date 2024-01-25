package com.river.connector.jms.model

enum class DeliveryMode(
    val value: Int
) {
    NON_PERSISTENT(1),
    PERSISTENT(2)
}
