package com.river.connector.ftp

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPSClient

object Ftp {
    fun server(
        host: String,
        port: Int = 21,
        configuration: FtpConfiguration.() -> Unit = { },
    ) = FtpConfiguration(host, port).also(configuration)

    fun download(
        remotePath: String,
        configuration: FtpConfiguration,
    ): Flow<Byte> =
        flow {
            val client =
                configuration
                    .secure
                    ?.let { FTPSClient(it.protocol, it.implicit) }
                    ?: FTPClient()

            with(client) {
                defineConfigurations(configuration)
                emitAll(retrieveFileAsFlow(remotePath).onCompletion { logout() })
            }
        }.flowOn(context)

    suspend fun upload(
        configuration: FtpConfiguration,
        remotePath: String,
        upstream: Flow<ByteArray>,
    ) {
        val client =
            configuration
                .secure
                ?.let { FTPSClient(it.protocol, it.implicit) }
                ?: FTPClient()

        with(client) {
            defineConfigurations(configuration)
            storeFileFromFlow(remotePath, upstream).collect()
            CoroutineScope(context).launch { logout() }.join()
        }
    }
}
