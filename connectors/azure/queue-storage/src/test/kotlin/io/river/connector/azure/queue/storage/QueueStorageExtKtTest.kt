package io.river.connector.azure.queue.storage

import com.azure.storage.queue.QueueAsyncClient
import com.azure.storage.queue.QueueClientBuilder
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.river.core.via
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull

class QueueStorageExtKtTest : FeatureSpec({
    feature("Azure queue storage as flow") {
        withClient("hello-world") {
            beforeTest {
                create().awaitFirstOrNull()
                clearMessages().awaitFirstOrNull()
            }

            scenario("Send messages") {
                (1..100)
                    .asFlow()
                    .map { SendMessageRequest(it.toString()) }
                    .via { sendMessagesFlow() }
                    .count() shouldBe 100

                messageCount() shouldBe 100
            }

            scenario("Receive messages") {
                (1..100)
                    .asFlow()
                    .map { SendMessageRequest(it.toString()) }
                    .via { sendMessagesFlow() }
                    .count() shouldBe 100

                receiveMessagesAsFlow()
                    .take(100)
                    .count() shouldBe 100

                messageCount() shouldBe 100
            }

            scenario("Delete messages") {
                (1..100)
                    .asFlow()
                    .map { SendMessageRequest(it.toString()) }
                    .via { sendMessagesFlow() }
                    .count() shouldBe 100

                val messages =
                    receiveMessagesAsFlow()
                        .take(100)
                        .toList()

                messages shouldHaveSize 100

                messageCount() shouldBe 100

                messages
                    .asFlow()
                    .via { deleteMessagesFlow() }
                    .count() shouldBe 100

                messageCount() shouldBe 0
            }
        }
    }
})

suspend fun QueueAsyncClient.messageCount() =
    properties
        .awaitFirst()
        .approximateMessagesCount

inline fun withClient(
    queueName: String,
    f: QueueAsyncClient.() -> Unit
) {
    QueueClientBuilder()
        .connectionString(
            "DefaultEndpointsProtocol=http;" +
                    "AccountName=devstoreaccount1;" +
                    "QueueEndpoint=http://localhost:10001/devstoreaccount1/hello/messages;" +
                    "AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw=="
        )
        .queueName(queueName)
        .buildAsyncClient()
        .also(f)
}
