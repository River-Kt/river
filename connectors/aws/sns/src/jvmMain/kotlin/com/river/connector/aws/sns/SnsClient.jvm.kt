package com.river.connector.aws.sns

import com.river.connector.aws.sns.internal.asEntry
import com.river.connector.aws.sns.model.PublishMessageRequest
import com.river.connector.aws.sns.model.PublishMessageResponse
import kotlinx.coroutines.future.await
import software.amazon.awssdk.services.sns.SnsAsyncClient
import software.amazon.awssdk.services.sns.model.PublishBatchResponse

actual fun interface SnsClient {
    actual suspend fun publishBatch(
        topicArn: String,
        messages: List<PublishMessageRequest>
    ): List<PublishMessageResponse>

    companion object {
        operator fun invoke(client: SnsAsyncClient) =
            SnsClient { topicArn, chunk ->
                val entries = chunk.mapIndexed { index, publishMessageRequest ->
                    publishMessageRequest.asEntry("$index")
                }

                val result: PublishBatchResponse =
                    client
                        .publishBatch { it.publishBatchRequestEntries(entries).topicArn(topicArn) }
                        .await()

                val successful = result.successful().map {
                    PublishMessageResponse.Successful(it.id(), it.messageId(), it.sequenceNumber())
                }

                val failed = result.failed().map {
                    PublishMessageResponse.Failure(it.id(), it.code(), it.message(), it.senderFault())
                }

                (successful + failed).sortedBy { it.id }
            }
    }
}
