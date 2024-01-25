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

        internal fun parse(eventLines: String): ServerSentEvent {
            var id: String? = null
            var event: String? = null
            val data = mutableListOf<String>()
            val comments = mutableListOf<String>()

            val lines = eventLines.split("\n")

            for (line in lines) {
                when {
                    line.startsWith("id:") ->
                        id = line.removePrefix("id: ").trim()

                    line.startsWith("event:") ->
                        event = line.removePrefix("event: ").trim()

                    line.startsWith("data:") ->
                        data.add(line.removePrefix("data: ").trim())

                    line.startsWith(":") ->
                        comments.add(line.removePrefix(":").trim())
                }
            }

            return ServerSentEvent(id, event, data, comments)
        }
    }
}
