package io.river.connector.twilio

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.river.connector.twilio.model.CreateMessage
import io.river.connector.twilio.model.Message
import io.river.connector.twilio.model.TwilioConfiguration
import io.river.connector.twilio.model.asHttpRequest
import io.river.util.http.ofString
import io.river.util.http.send
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
