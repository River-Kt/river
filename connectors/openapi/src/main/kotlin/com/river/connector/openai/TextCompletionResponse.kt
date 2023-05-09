package com.river.connector.openai

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

enum class Model(
    val type: String,
    val maxTokens: Int,
) {
    GPT_4("gpt-4", 4096),
    GPT_3_5_TURBO("gpt-3.5-turbo", 2048),
    GPT_3_5_TURBO_0301("gpt-3.5-turbo-0301", 2048);

    @JsonValue
    fun value() = type
}

data class Temperature(@JsonValue val value: Double = 1.0) {
    init {
        require(value <= 2) { "temperature cannot be higher than 2" }
        require(value >= 0) { "temperature cannot be lower than 0" }
    }
}
