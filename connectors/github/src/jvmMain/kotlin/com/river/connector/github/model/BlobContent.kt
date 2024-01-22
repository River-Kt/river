@file:OptIn(ExperimentalEncodingApi::class)

package com.river.connector.github.model

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

data class BlobContent(
    val sha: String,
    val nodeId: String,
    val size: Long,
    val url: String,
    val content: String,
    val encoding: Encoding
) {
    enum class Encoding { `UTF-8`, BASE64 }

    val decodedContent: String by lazy {
        when (encoding) {
            Encoding.`UTF-8` -> content
            Encoding.BASE64 -> {
                String(Base64.decode(content.replace("\n", "")))
            }
        }
    }
}
