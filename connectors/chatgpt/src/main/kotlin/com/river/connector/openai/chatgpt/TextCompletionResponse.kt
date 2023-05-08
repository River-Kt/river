package com.river.connector.openai.chatgpt

import com.fasterxml.jackson.annotation.JsonValue

data class TextCompletionResponse(
    val id: String,
    val objectType: String?,
    val created: Long,
    val choices: List<Choice>,
    val model: String
)

data class Choice(
    val delta: Delta,
    val index: Int,
    val logprobs: Int?,
    val finishReason: String?
) {
    data class Delta(
        val content: String?,
        val role: Role?
    )
}

enum class Role {
    SYSTEM, USER, ASSISTANT;

    @JsonValue
    fun value() = name.lowercase()
}

data class ChatCompletion(
    val messages: List<Message>,
    val model: Model = Model.GPT_3_5_TURBO,
    val temperature: Temperature = Temperature(),
) {
    val stream: Boolean = true

    data class Message(
        val content: String,
        val role: Role = Role.USER,
        val name: String? = null
    )
}
