package com.river.connector.google.drive

data class FileList(
    val kind: String,
    val incompleteSearch: Boolean,
    val files: List<GoogleDriveFile>,
    val nextPageToken: String?
)

data class GoogleDriveFile(
    val kind: String,
    val mimeType: String,
    val id: String,
    val name: String,
    val resourceKey: String
)
