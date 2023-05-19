package com.river.connector.openai.model

data class Items<T>(
    val `object`: String,
    val data: List<T>
)
