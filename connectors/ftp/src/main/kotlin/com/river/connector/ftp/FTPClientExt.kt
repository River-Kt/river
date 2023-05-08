package com.river.connector.ftp

import com.river.connector.file.asFlow
import com.river.connector.file.asInputStream
import com.river.connector.ftp.model.FtpConfiguration
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPSClient
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(FTPSClient::class.java)

/**
 * Establishes a connection to an FTP server specified by [host] and [port].
 *
 * If [port] is not specified, it defaults to 21.
 *
 * [configuration] is a lambda that can be used to configure the connection using an instance of
 * [FtpConfiguration]. This lambda is optional and can be omitted if no additional configuration
 * is needed.
 *
 * Returns a [Flow] that emits an instance of [FTPClient] once the connection has been established.
 *
 * Example usage:
 *
 * ```
 * connect("ftp.example.com") {
 *     credentials {
 *         username = "username"
 *         password = "password"
 *     }
 *     binary = true
 * }.collect { ftpClient ->
 *     // Use the FTPClient instance
 * }
 * ```
 */
fun connect(
    host: String,
    port: Int = 21,
    configuration: FtpConfiguration.() -> Unit = { }
): Flow<FTPClient> =
    FtpConfiguration(host, port)
        .also(configuration)
        .let(::connect)

/**
 * Establishes a connection to an FTP server using the specified [configuration].
 *
 * [configuration] is an instance of [FtpConfiguration] that specifies the configuration options
 * for the connection.
 *
 * Returns a [Flow] that emits an instance of [FTPClient] once the connection has been established.
 *
 * Example usage:
 *
 * ```
 * val configuration = FtpConfiguration("ftp.example.com").apply {
 *     credentials {
 *         username = "username"
 *         password = "password"
 *     }
 *     binary = true
 * }
 *
 * connect(configuration).collect { ftpClient ->
 *     // Use the FTPClient instance
 * }
 * ```
 */
fun connect(configuration: FtpConfiguration): Flow<FTPClient> =
    flow {
        val properties = mapOf(
            "username" to (configuration.credentials?.username ?: "none"),
            "password" to (configuration.credentials?.password?.let { "<hidden>" } ?: "none"),
            "host" to configuration.host,
            "port" to configuration.port,
            "passiveMode" to configuration.passiveMode,
            "binary" to configuration.binary,
            "secure_protocol" to (configuration.secure?.protocol ?: "none")
        ).toList().joinToString { (k, v) -> "$k=$v" }

        logger.info("Initializing client for with properties: $properties")

        val ftpClient =
            configuration
                .secure
                ?.let { FTPSClient(it.protocol, it.implicit) }
                ?: FTPClient()

        if (configuration.passiveMode)
            ftpClient.enterLocalPassiveMode()
        if (configuration.binary)
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE)

        ftpClient.connect(configuration.host, configuration.port)

        logger.info("Connected!")

        configuration.credentials?.let {
            logger.info("Logging...")
            ftpClient.login(it.username, it.password)
            logger.info("Login finished!")
        }

        emit(ftpClient)
    }.flowOn(Dispatchers.IO)

/**
 * Downloads a file from the remote FTP server specified by [remotePath].
 *
 * Returns a [Flow] that emits each byte of the file content as it is downloaded.
 *
 * Example usage:
 *
 * ```
 * ftpClient.download("/path/to/remote/file").collect { byte ->
 *     // Process the downloaded byte
 * }
 * ```
 */
fun FTPClient.download(
    remotePath: String,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
): Flow<ByteArray> =
    flow {
        logger.info("Retrieving file as input stream...")
        val retrieveFileStream = retrieveFileStream(remotePath)

        if (retrieveFileStream == null) {
            logger.info("Got empty result, emitting an empty flow.")
            emitAll(emptyFlow())
        } else {
            logger.info("Preparing to emit file...")
            val flow = retrieveFileStream.asFlow(dispatcher)
            emitAll(flow)
            logger.info("Done emitting the retrieved file!")
        }
    }.flowOn(dispatcher)

/**
 * Uploads a file to the remote FTP server specified by [remotePath] using the specified [content].
 *
 * [content] is a [Flow] that emits byte arrays that represent the content of the file to be uploaded.
 *
 * Returns a [Flow] that emits a single [Unit] value once the file has been uploaded.
 *
 * Example usage:
 *
 * ```
 * val content = flowOf(byteArrayOf(0x01, 0x02, 0x03))
 *
 * ftpClient.upload("/path/to/remote/file", content).collect {
 *     // File has been uploaded
 * }
 * ```
 */
fun FTPClient.upload(
    remotePath: String,
    content: Flow<ByteArray>,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
): Flow<Unit> =
    flow {
        logger.info("Uploading the file to $remotePath...")

        content
            .asInputStream(dispatcher = dispatcher)
            .use { storeFile(remotePath, it) }

        logger.info("Done uploading.")
        emit(Unit)
    }.flowOn(dispatcher)
