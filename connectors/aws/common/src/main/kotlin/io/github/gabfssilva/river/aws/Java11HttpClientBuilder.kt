package io.github.gabfssilva.river.aws

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import software.amazon.awssdk.http.async.SdkAsyncHttpClient
import software.amazon.awssdk.utils.AttributeMap
import java.net.http.HttpClient

class Java11HttpClientBuilder(
    private val httpClient: HttpClient,
    private val scope: CoroutineScope
) : SdkAsyncHttpClient.Builder<Java11HttpClientBuilder> {
    private fun copy(
        httpClient: HttpClient = this.httpClient,
        scope: CoroutineScope = this.scope
    ) = Java11HttpClientBuilder(httpClient, scope)

    override fun buildWithDefaults(serviceDefaults: AttributeMap?): SdkAsyncHttpClient =
        Java11HttpClient(httpClient, scope)

    fun withHttpClient(httpClient: HttpClient) =
        copy(httpClient = httpClient)

    fun withCoroutineScope(scope: CoroutineScope) =
        copy(scope = scope)
}
