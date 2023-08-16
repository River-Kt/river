package com.river.connector.openai.model

import com.river.core.ExperimentalRiverApi

@ExperimentalRiverApi
data class Items<T>(
    val `object`: String,
    val data: List<T>
)
