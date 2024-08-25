@file:OptIn(ExperimentalEncodingApi::class, ExperimentalSerializationApi::class)

package com.river.connector.google.internal

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.asymmetric.RSA
import dev.whyoleg.cryptography.algorithms.digest.SHA256
import dev.whyoleg.cryptography.operations.signature.SignatureGenerator
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.util.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.*
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class CachedToken(
    private val serviceAccount: JsonObject,
    private val scope: String,
    private val httpClient: HttpClient = DefaultHttpClient,
) {
    companion object {
        private val DefaultHttpClient: HttpClient = HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    namingStrategy = JsonNamingStrategy.SnakeCase
                    ignoreUnknownKeys = true
                })
            }
        }
    }

    private data class Token(
        val expiredAt: Instant,
        val value: String
    ) {
        fun expired() = expiredAt <= Clock.System.now()
    }

    private lateinit var current: Token

    private val mutex = Mutex()

    suspend fun currentToken(): String {
        val invalid = { !this::current.isInitialized || current.expired() }

        val refresh = suspend {
            mutex.withLock { if (invalid()) current = getToken(scope) }
        }

        if (invalid()) refresh()

        return current.value
    }

    private fun ByteArray.encodeBase64(): String = Base64.encode(this)

    private suspend fun createJwt(
        issuedAt: Instant,
        privateKey: SignatureGenerator,
        scope: String
    ): String {
        val header = buildJsonObject {
            put("alg", "RS256")
            put("typ", "JWT")
        }

        val payload = buildJsonObject {
            put("iss", serviceAccount["client_email"] ?: JsonNull)
            put("aud", "https://oauth2.googleapis.com/token")
            put("iat", issuedAt.epochSeconds)
            put("exp", (issuedAt + 5.minutes).epochSeconds)
            put("scope", scope)
        }

        val unsignedToken = "${header.toString().encodeBase64()}.${payload.toString().encodeBase64()}"
        val signature = privateKey.generateSignature(unsignedToken.encodeToByteArray())

        return "$unsignedToken.${signature.encodeBase64()}"
    }

    private suspend fun getToken(scope: String): Token {
        val privateKey =
            serviceAccount["private_key"]
                ?.jsonPrimitive
                ?.content
                ?.let {
                    val rsa = CryptographyProvider.Default.get(RSA.PKCS1)

                    rsa.privateKeyDecoder(SHA256)
                        .decodeFrom(RSA.PrivateKey.Format.PEM.Generic, it.encodeToByteArray())
                        .signatureGenerator()
                } ?: error("Could not extract private key")

        val issuedAt = Clock.System.now()

        val tokenUrl = serviceAccount.getValue("token_uri").jsonPrimitive.content

        val jwt = createJwt(issuedAt, privateKey, scope)

        val response: HttpResponse = httpClient.submitForm(
            url = tokenUrl,
            formParameters = parameters {
                append("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
                append("assertion", jwt)
            }
        )

        require(response.status != HttpStatusCode.Unauthorized) { "The provided credentials are not valid." }
        require(response.status == HttpStatusCode.OK) { "Received HTTP status: ${response.status.value} -- ${response.bodyAsText()}" }

        val jsonBody = response.body<JsonObject>()

        return Token(
            expiredAt = issuedAt + jsonBody.getValue("expires_in").jsonPrimitive.int.seconds,
            value = jsonBody.getValue("access_token").jsonPrimitive.content
        )
    }
}
