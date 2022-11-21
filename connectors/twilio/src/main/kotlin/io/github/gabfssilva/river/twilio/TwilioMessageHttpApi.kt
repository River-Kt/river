package io.github.gabfssilva.river.twilio

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.gabfssilva.river.util.http.ofString
import io.github.gabfssilva.river.util.http.send
import java.net.http.HttpClient

class TwilioMessageHttpApi(
    val configuration: TwilioConfiguration,
    val objectMapper: ObjectMapper = jacksonObjectMapper(),
    val httpClient: HttpClient = HttpClient.newHttpClient(),
) {
    private val messageUrl =
        "${configuration.baseUrl}/2010-04-01/Accounts/${configuration.accountSid}/Messages.json"

    suspend fun createMessage(
        createMessage: CreateMessage
    ): Message =
        createMessage
            .asHttpRequest(messageUrl, configuration.authenticationHeader)
            .send(ofString, httpClient)
            .let { response ->
                assert(response.statusCode() == 200) {
                    "Twilio responded a unexpected status code: ${response.statusCode()}"
                }

                objectMapper.readValue(response.body())
            }
}
