package com.river.connector.github.model

sealed interface Content {
    val path: String

    data class File(
        val type: String,
        val encoding: String,
        val size: Int,
        val name: String,
        override val path: String,
        val content: String,
        val sha: String,
        val url: String,
        val gitUrl: String,
        val htmlUrl: String,
        val downloadUrl: String,
        val links: Map<String, String>
    ) : Content

    data class Dir(
        override val path: String,
        val list: List<Content>
    ) : Content
}
