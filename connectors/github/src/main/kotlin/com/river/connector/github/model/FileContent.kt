package com.river.connector.github.model

data class FileContent(
    val path: String,
    val fileType: String,
    val content: String
)
