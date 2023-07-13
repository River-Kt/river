package com.river.connector.apache.kafka

class PropertyBuilder {
    private val properties: MutableMap<String, Any> = mutableMapOf()

    infix fun String.to(value: Any) {
        properties[this] = value
    }

    fun properties() = properties
}
