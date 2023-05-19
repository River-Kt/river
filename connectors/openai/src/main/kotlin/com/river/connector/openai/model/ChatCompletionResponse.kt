package com.river.connector.openai.model

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

data class CompletionRequest(
    val prompt: List<String>,
    val maxTokens: Int = 16,
    val model: String = Models.gpt_3_5_turbo,
    val temperature: Temperature = Temperature(),
) {
    val stream = true

    companion object {
        operator fun invoke(
            vararg prompt: String,
            maxTokens: Int = 16,
            model: String = Models.gpt_3_5_turbo,
            temperature: Temperature = Temperature(),
        ) = CompletionRequest(prompt.toList(), maxTokens, model, temperature)
    }
}

data class ChatCompletionRequest(
    val messages: List<Message>,
    val model: String = Models.gpt_3_5_turbo,
    val temperature: Temperature = Temperature(),
) {
    val stream: Boolean = true

    data class Message(
        val content: String,
        val role: Role = Role.USER,
        val name: String? = null
    )
}

object Models {
    const val gpt_4 = "gpt-4"
    const val gpt_3_5_turbo = "gpt-3.5-turbo"
    const val gpt_3_5_turbo_0301 = "gpt-3.5-turbo-0301"
}

data class Temperature(@JsonValue val value: Double = 1.0) {
    init {
        require(value <= 2) { "temperature cannot be higher than 2" }
        require(value >= 0) { "temperature cannot be lower than 0" }
    }
}
