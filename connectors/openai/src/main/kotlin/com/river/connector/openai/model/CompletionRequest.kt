package com.river.connector.openai.model

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

data class CompletionResponse(
    val id: String,
    val `object`: String,
    val created: Int,
    val model: String,
    val choices: List<Choice>,
    val usage: Usage?
) {
    data class Choice(
        val text: String,
        val index: Int,
        val logprobs: Int?,
        val finishReason: String?
    )

    data class Usage(
        val promptTokens: Int,
        val completionTokens: Int,
        val totalTokens: Int
    )
}
