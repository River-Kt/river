package com.river.connector.google.drive.internal

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.fasterxml.jackson.module.kotlin.readValue
import com.river.connector.format.json.defaultObjectMapper
import com.river.connector.http.coSend
import com.river.connector.http.ofString
import com.river.connector.http.post
import com.river.core.withPromise
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.actor
import java.net.http.HttpClient
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*

internal class BearerTokenActor(
    private val serviceAccount: JsonNode,
    private val httpClient: HttpClient,
    private val scope: CoroutineScope
) {
    private val actor = scope.tokenActor()

    suspend fun currentToken() =
        withPromise { promise -> actor.send(GetBearerToken(promise)) }

    private class GetBearerToken(val completableDeferred: CompletableDeferred<String>)

    private fun CoroutineScope.tokenActor() = actor<GetBearerToken> {
        var fetchAt = System.currentTimeMillis()
        var last = getToken(fetchAt, serviceAccount, httpClient)

        for (message in channel) {
            if (last.expired(fetchAt)) {
                fetchAt = System.currentTimeMillis()
                last = getToken(fetchAt, serviceAccount, httpClient)
            }

            message.completableDeferred.complete(last.accessToken)
        }
    }

    private suspend fun getToken(
        now: Long,
        serviceAccount: JsonNode,
        httpClient: HttpClient,
    ): AccessTokenResponse {
        val privateKey =
            serviceAccount["private_key"]
                .asText()
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("\n", "")
                .let { Base64.getDecoder().decode(it) }
                .let { PKCS8EncodedKeySpec(it) }
                .let {
                    val keyFactory = KeyFactory.getInstance("RSA")
                    val privateKey = keyFactory.generatePrivate(it) as RSAPrivateKey
                    privateKey
                }

        val algorithm = Algorithm.RSA256(null, privateKey)

        val tokenUrl = "https://oauth2.googleapis.com/token"

        val jwt: String = JWT.create()
            .withIssuer(serviceAccount["client_id"].asText())
            .withSubject(serviceAccount["client_id"].asText())
            .withAudience(tokenUrl)
            .withIssuedAt(Date(now))
            .withExpiresAt(Date(now + 3600 * 1000))
            .withClaim("scope", "https://www.googleapis.com/auth/drive")
            .sign(algorithm)

        val body = mapOf(
            "grant_type" to "urn:ietf:params:oauth:grant-type:jwt-bearer",
            "assertion" to jwt
        )

        val response = httpClient.coSend(
            request = post(tokenUrl) {
                contentType("application/json")
                stringBody(defaultObjectMapper.writeValueAsString(body))
            },
            bodyHandler = ofString
        )

        require(response.statusCode() != 401) { "The provided credentials is not valid." }
        require(response.statusCode() == 200) { "Received HTTP status: ${response.statusCode()}" }

        return defaultObjectMapper.readValue(response.body())
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
    data class AccessTokenResponse(
        val accessToken: String,
        val expiresIn: Int,
        val tokenType: String
    ) {
        fun expired(start: Long) =
            (start + expiresIn) <= System.currentTimeMillis()
    }
}
