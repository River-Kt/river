package io.river.connector.jms

enum class DeliveryMode(
    val value: Int
) {
    NON_PERSISTENT(1),
    PERSISTENT(2)
}
