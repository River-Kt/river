package com.river.connector.openai.model

import com.fasterxml.jackson.annotation.JsonValue

data class Temperature(@JsonValue val value: Double = 1.0) {
    init {
        require(value <= 2) { "temperature cannot be higher than 2" }
        require(value >= 0) { "temperature cannot be lower than 0" }
    }
}
