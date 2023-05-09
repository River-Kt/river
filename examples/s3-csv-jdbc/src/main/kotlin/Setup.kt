import com.river.connector.rdbms.jdbc.Jdbc
import com.river.connector.rdbms.jdbc.singleUpdate
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.future.await
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient
import java.net.URI

val s3AsyncClient: S3AsyncClient =
    S3AsyncClient
        .builder()
        .endpointOverride(URI("http://s3.localhost.localstack.cloud:4566"))
        .region(Region.US_EAST_1)
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create("x", "x")
            )
        )
        .build()

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
    s3AsyncClient.createBucket { it.bucket("bucket") }.await()

