@file:OptIn(ExperimentalCoroutinesApi::class)

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.createBucket
import aws.smithy.kotlin.runtime.net.url.Url
import com.river.connector.rdbms.jdbc.Jdbc
import com.river.connector.rdbms.jdbc.singleUpdate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.future.await

val s3 =
    S3Client {
        endpointUrl = Url.parse("http://s3.localhost.localstack.cloud:4566")
        region = "us-east-1"
        credentialsProvider = StaticCredentialsProvider {
            accessKeyId = "x"
            secretAccessKey = "x"
        }
    }


val jdbc: Jdbc =
    Jdbc(
        url = "jdbc:postgresql://localhost:5432/numbers",
        credentials = "postgresql" to "postgresql",
    )

suspend fun createTable() {
    jdbc
        .singleUpdate("create table if not exists numbers (n integer, ntimes2 integer);")
        .single()

    jdbc.singleUpdate("delete from numbers;")
        .single()
}

suspend fun createBucket(name: String) =
    s3.createBucket { bucket = "bucket" }
