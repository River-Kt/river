@file:OptIn(DelicateCoroutinesApi::class)

package com.river.connector.ftp

import com.river.connector.file.asFlow
import com.river.connector.file.writeTo
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient

fun FTPClient.retrieveFileAsFlow(remotePath: String): Flow<Byte> =
    retrieveFileStream(remotePath)
        .asFlow(context)

fun FTPClient.storeFileFromFlow(
    remotePath: String,
    content: Flow<ByteArray>
) = flow {
    content.writeTo(context) { storeFileStream(remotePath) }
    emit(Unit)
}.flowOn(context)

internal val context by lazy { newSingleThreadContext("FTPContext") }

internal fun FTPClient.defineConfigurations(configuration: FtpConfiguration) {
    connect(configuration.host, configuration.port)

    if (configuration.passiveMode)
        enterLocalPassiveMode()
    if (configuration.binary)
        setFileType(FTP.BINARY_FILE_TYPE)

    configuration.credentials?.let { login(it.username, it.password) }
}
