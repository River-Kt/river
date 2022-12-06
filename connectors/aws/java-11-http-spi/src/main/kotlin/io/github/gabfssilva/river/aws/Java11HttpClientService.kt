package io.github.gabfssilva.river.aws

import software.amazon.awssdk.http.async.SdkAsyncHttpClient
import software.amazon.awssdk.http.async.SdkAsyncHttpService

class Java11HttpClientService : SdkAsyncHttpService {
    override fun createAsyncHttpClientFactory(): SdkAsyncHttpClient.Builder<*> =
        Java11HttpClient.builder()
}
