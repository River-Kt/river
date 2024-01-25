package com.river.connector.ftp

import com.river.core.asString
import com.river.core.joinToString
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.single
import org.junit.jupiter.api.assertDoesNotThrow

class FTPClientExtKtTest : FeatureSpec({
    feature("FTP operations as Flow") {
        withServer { port ->
            suspend fun client() =
                connect("localhost", port) {
                    credentials {
                        username = "admin"
                        password = "admin"
                    }
                }.single()

            scenario("FTP connect works properly") {
                assertDoesNotThrow { client() }
            }

            scenario("FTP upload") {
                val client = client()

                val bytes = flowOf("hello, world!".encodeToByteArray())

                assertDoesNotThrow { client.upload("/data/hello.txt", bytes).single() }
            }

            scenario("FTP download") {
                val client = client()

                val bytes = flowOf("hello, world!".encodeToByteArray())

                client.upload("/data/hello.txt", bytes).single()

                client
                    .download("/data/hello.txt")
                    .asString()
                    .joinToString() shouldBe "hello, world!"
            }
        }
    }
})
