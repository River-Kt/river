package com.river.connector.google.drive

class FilesListQuery {
    var corpora: String = "user"
    var driveId: String? = null
    var includeItemsFromAllDrives: Boolean = false
    var orderBy: String? = null
    var pageSize: Int = 100
    var pageToken: String? = null
    var spaces: String? = null
    var supportsAllDrives: Boolean = false
    var includePermissionsForView: String? = null
    var includeLabels: String? = null

    fun build(): Map<String, Any?> = mapOf(
        "corpora" to corpora,
        "driveId" to driveId,
        "includeItemsFromAllDrives" to includeItemsFromAllDrives,
        "orderBy" to orderBy,
        "pageSize" to pageSize,
        "pageToken" to pageToken,
        "spaces" to spaces,
        "supportsAllDrives" to supportsAllDrives,
        "includePermissionsForView" to includePermissionsForView,
        "includeLabels" to includeLabels,
    )
}
