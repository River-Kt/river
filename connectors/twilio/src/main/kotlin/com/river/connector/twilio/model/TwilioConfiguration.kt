package com.river.connector.twilio.model

import java.util.*

class TwilioConfiguration(
    val accountSid: String,
    val authToken: String,
    val baseUrl: String = BaseUrl,
) {
    val authenticationHeader: String =
        Base64
            .getEncoder()
            .encodeToString("$accountSid:$authToken".encodeToByteArray())

    companion object {
        const val BaseUrl = "https://api.twilio.com"
    }
}

