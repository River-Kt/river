package com.river.connector.google.drive

import kotlinx.serialization.Serializable

@Serializable
data class FileList(
    val kind: String,
    val incompleteSearch: Boolean,
    val files: List<GoogleDriveFile>,
    val nextPageToken: String? = null
)

@Serializable
data class GoogleDriveFile(
    val kind: String,
    val mimeType: String,
    val id: String,
    val name: String,
    val resourceKey: String
)
