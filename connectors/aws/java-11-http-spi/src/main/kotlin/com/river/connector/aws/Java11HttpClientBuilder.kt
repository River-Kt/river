package com.river.connector.aws

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import software.amazon.awssdk.http.Protocol
import software.amazon.awssdk.http.SdkHttpConfigurationOption.CONNECTION_TIMEOUT
import software.amazon.awssdk.http.SdkHttpConfigurationOption.PROTOCOL
import software.amazon.awssdk.http.async.SdkAsyncHttpClient
import software.amazon.awssdk.utils.AttributeMap
import java.net.http.HttpClient
import java.net.http.HttpClient.Version

class Java11HttpClientBuilder(
    private val httpClient: HttpClient? = null,
    private val scope: CoroutineScope? = null
) : SdkAsyncHttpClient.Builder<Java11HttpClientBuilder> {
    private fun copy(
        httpClient: HttpClient? = this.httpClient,
        scope: CoroutineScope? = this.scope
    ) = Java11HttpClientBuilder(httpClient, scope)

    override fun buildWithDefaults(serviceDefaults: AttributeMap): SdkAsyncHttpClient =
        Java11HttpClient(
            httpClient = httpClient ?: defaultHttpClient(serviceDefaults),
            scope = scope ?: CoroutineScope(Dispatchers.Default),
            attributes = serviceDefaults
        )

    fun withHttpClient(httpClient: HttpClient) =
        copy(httpClient = httpClient)

    fun withCoroutineScope(scope: CoroutineScope) =
        copy(scope = scope)

    private fun defaultHttpClient(
        defaults: AttributeMap
    ): HttpClient =
        HttpClient
            .newBuilder()
            .version(
                when (defaults.get(PROTOCOL)) {
                    Protocol.HTTP1_1 -> Version.HTTP_1_1
                    else -> Version.HTTP_2
                }
            )
            .apply {
                defaults.get(CONNECTION_TIMEOUT)?.also {
                    connectTimeout(it)
                }
            }
            .build()
}
