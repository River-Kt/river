package io.github.gabfssilva.river.jms

enum class DeliveryMode(
    val value: Int
) {
    NON_PERSISTENT(1),
    PERSISTENT(2)
}
