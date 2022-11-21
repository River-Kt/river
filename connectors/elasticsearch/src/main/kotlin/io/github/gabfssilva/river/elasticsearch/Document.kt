package io.github.gabfssilva.river.elasticsearch

import com.fasterxml.jackson.databind.JsonNode

data class Document<T>(
    val id: String,
    val index: String,
    val document: T
)
