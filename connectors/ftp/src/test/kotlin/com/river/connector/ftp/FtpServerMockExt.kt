package com.river.connector.ftp

import org.mockftpserver.fake.FakeFtpServer
import org.mockftpserver.fake.UserAccount
import org.mockftpserver.fake.filesystem.DirectoryEntry
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem

fun server(
    username: String = "admin",
    password: String = "admin",
    port: Int = 21,
    homeDir: String = "/data"
): FakeFtpServer =
    FakeFtpServer().also { fakeFtpServer ->
        fakeFtpServer.addUserAccount(UserAccount(username, password, homeDir))

        fakeFtpServer.fileSystem = UnixFakeFileSystem().also { it.add(DirectoryEntry(homeDir)) }
        fakeFtpServer.serverControlPort = port
    }

inline fun <T> withServer(
    username: String = "admin",
    password: String = "admin",
    port: Int = 21,
    homeDir: String = "/data",
    f: () -> T
): T {
    val server = server(username, password, port, homeDir)
    server.start()
    return runCatching(f).also { server.stop() }.getOrThrow()
}
