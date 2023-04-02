package com.river.connector.ftp

class FtpConfiguration(
    internal val host: String,
    internal val port: Int,
    internal var secure: Secure? = null,
    var credentials: Credentials? = null,
    var passiveMode: Boolean = false,
    var binary: Boolean = false
) {
    fun secure(f: Secure.() -> Unit = {}) {
        secure = Secure().also(f)
    }

    fun credentials(f: Credentials.() -> Unit) {
        credentials = Credentials().also(f)
    }

    class Secure(
        var protocol: String = "TLS",
        var implicit: Boolean = false
    )

    class Credentials(
        var username: String? = null,
        var password: String? = null
    )
}
