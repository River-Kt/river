package com.river.connector.file.model

import com.river.core.ExperimentalRiverApi
import java.util.zip.ZipEntry

/**
 * Represents a ZIP entry with its content.
 *
 * @param entry The original [ZipEntry].
 * @param data The byte content of the entry.
 */
@ExperimentalRiverApi
class ContentfulZipEntry(entry: ZipEntry, val data: ByteArray) : ZipEntry(entry)
