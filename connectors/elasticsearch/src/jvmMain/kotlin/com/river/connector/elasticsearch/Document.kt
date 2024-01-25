package com.river.connector.elasticsearch

import com.river.core.ExperimentalRiverApi

@ExperimentalRiverApi
data class Document<T>(
    val id: String,
    val index: String,
    val document: T
)
