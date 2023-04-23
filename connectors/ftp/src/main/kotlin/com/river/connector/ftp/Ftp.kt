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

    /**
     * This function downloads a file from a remote FTP server using the given FTP configuration.
     *
     * @param remotePath The path of the file to download from the remote server.
     * @param configuration The FTP configuration to be used for the download.
     *
     * @return A flow of bytes representing the downloaded file.
     **
     * Example usage:
     *
     * ```
     *  val ftpConfig = FtpConfiguration(
     *       host = "ftp.example.com",
     *       port = 21,
     *       username = "user",
     *       password = "password",
     *       secure = null
     *  )
     *
     *  download("/path/to/file.txt", ftpConfig).collect{
     *       // Do something with the downloaded file content
     *  }
     * ```
     */
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

    /**
     * Uploads a file to a remote FTP server.
     *
     * @param configuration the configuration for the FTP server.
     * @param remotePath the remote path where the file should be stored.
     * @param upstream the flow of bytes to upload.
     * @throws IOException if an I/O error occurs while communicating with the FTP server.
     * @throws IllegalStateException if the FTP client is not connected.
     *
     * Example usage:
     *
     * ```
     *  val ftpConfiguration = FtpConfiguration("example.com", "username", "password")
     *  val flowOfBytes = // generate your flow of bytes
     *  upload(ftpConfiguration, "/path/to/remote/file.txt", flowOfBytes)
     * ```
     */
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
