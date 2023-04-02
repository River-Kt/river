package com.river.connector.aws

import software.amazon.awssdk.http.async.SdkAsyncHttpClient
import software.amazon.awssdk.http.async.SdkAsyncHttpService

class Java11HttpClientService : SdkAsyncHttpService {
    override fun createAsyncHttpClientFactory(): SdkAsyncHttpClient.Builder<*> =
        com.river.connector.aws.Java11HttpClient.builder()
}
