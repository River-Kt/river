package io.github.gabfssilva.river.twilio

import com.fasterxml.jackson.annotation.JsonProperty
import java.net.URI
import java.net.http.HttpRequest

sealed class Destination(
    val value: String
) {
    data class Sms(val number: String) : Destination("sms:+$number")
    data class Whatsapp(val number: String) : Destination("whatsapp:+$number")
}

data class CreateMessage(
    val body: String,
    val messagingServiceSid: String,
    val to: Destination,
    val shortenUrls: Boolean = true,
) {
    fun asEncodedString() = "Body=$body" +
            "&MessagingServiceSid=$messagingServiceSid" +
            "&ShortenUrls=$shortenUrls" +
            "&To=$to"
}

data class Message(
    @JsonProperty("account_sid") val accountSid: String,
    @JsonProperty("api_version") val apiVersion: String,
    @JsonProperty("body") val body: String,
    @JsonProperty("date_created") val dateCreated: String,
    @JsonProperty("date_sent") val dateSent: String,
    @JsonProperty("date_updated") val dateUpdated: String,
    @JsonProperty("direction") val direction: String,
    @JsonProperty("error_code") val errorCode: Any,
    @JsonProperty("error_message") val errorMessage: Any,
    @JsonProperty("from") val from: String,
    @JsonProperty("messaging_service_sid") val messagingServiceSid: String,
    @JsonProperty("num_media") val numMedia: String,
    @JsonProperty("num_segments") val numSegments: String,
    @JsonProperty("price") val price: Any,
    @JsonProperty("price_unit") val priceUnit: Any,
    @JsonProperty("sid") val sid: String,
    @JsonProperty("status") val status: String,
    @JsonProperty("subresource_uris") val subresourceUris: SubresourceUris,
    @JsonProperty("to") val to: String,
    @JsonProperty("uri") val uri: String
) {
    data class SubresourceUris(
        @JsonProperty("media") val media: String
    )
}

fun CreateMessage.asHttpRequest(
    uri: URI,
    authorization: String
): HttpRequest =
    HttpRequest
        .newBuilder(uri)
            .POST(HttpRequest.BodyPublishers.ofString(asEncodedString()))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Authorization", authorization)
        .build()
