package com.river.connector.http

data class ServerSentEvent(
    val id: String? = null,
    val event: String? = null,
    val data: List<String>,
    val comments: List<String> = listOf()
) {
    companion object {
        operator fun invoke(data: String) =
            ServerSentEvent(null, null, listOf(data))
    }
}
