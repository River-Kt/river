package io.github.gabfssilva.river.twilio

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.future.await
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpResponse.BodyHandlers

class TwilioMessageHttpApi(
    val configuration: TwilioConfiguration,
    val objectMapper: ObjectMapper = jacksonObjectMapper(),
    val httpClient: HttpClient = HttpClient.newHttpClient(),
) {
    val messageUrl =
        URI("${configuration.baseUrl}/2010-04-01/Accounts/${configuration.accountSid}/Messages.json")

    suspend fun createMessage(
        createMessage: CreateMessage
    ): Message {
        val response =
            httpClient
                .sendAsync(
                    createMessage.asHttpRequest(messageUrl, configuration.authenticationHeader),
                    BodyHandlers.ofInputStream()
                )
                .await()

        assert(response.statusCode() == 200) {
            "Twilio responded a unexpected status code: ${response.statusCode()}"
        }

        return objectMapper.readValue(response.body())
    }
}
