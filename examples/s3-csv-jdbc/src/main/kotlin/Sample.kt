import com.river.connector.aws.s3.download
import com.river.connector.aws.s3.upload
import com.river.connector.format.csv.csv
import com.river.connector.format.csv.parseCsvWithHeaders
import com.river.connector.rdbms.jdbc.batchUpdate
import com.river.connector.rdbms.jdbc.query
import com.river.core.*
import kotlinx.coroutines.flow.*
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import kotlin.time.Duration.Companion.milliseconds

suspend fun main() {
    // For the sake of this test, we create a bucket and a database table
    createBucket("bucket")
    createTable()

    // A data class to map our simple domain
    data class Num(
        val n: Int,
        val nTimes2: Int
    )

    // For testing purposes, we're building a CSV file with 100000 entries
    // We create a flow and use intersperse("\n") to add a newline between each element
    // asByteArray() then converts the flow into a byte array for uploading
    s3AsyncClient
        .upload(
            bucket = "bucket",
            key = "numbers.csv",
            upstream = (1..100000)
                .asFlow()
                .map { Num(it, it * 2) }
                .csv(true)
                .intersperse("\n")
                .asByteArray()
        )
        .collect()

    // The download function emits a single tuple: the GetObjectResponse and the file's content as bytes
    // This can be useful when you want to examine the metadata of the file before downloading it
    val (metadata: GetObjectResponse, bytes: Flow<ByteArray>) =
        s3AsyncClient
            .download("bucket", "numbers.csv")
            .single()

    // The file won't be downloaded until we collect the bytes flow
    bytes
        // First, we transform the byte flow into a string flow
        .asString()
        // By calling lines(), we ensure that as soon as a line is parsed, it's emitted as an element
        .lines()
        // Then we parse it using parseCsvWithHeaders
        // The provided object is a Map<String, String>, so we extract the necessary data to build a Num instance again
        .parseCsvWithHeaders { Num(checkNotNull(it["n"]).toInt(), checkNotNull(it["nTimes2"]).toInt()) }
        .let { flow ->
            // Here, we use the JDBC connector to batch insert the elements into the numbers table
            // It's using time window strategy for grouping our updates, alongside with the parallelism level to 10
            jdbc.batchUpdate(
                sql = "insert into numbers (n, ntimes2) values (?, ?)",
                upstream = flow,
                groupStrategy = GroupStrategy.TimeWindow(50, 100.milliseconds),
                parallelism = 10
            ) {
                setInt(1, it.n)
                setInt(2, it.nTimes2)
            }
        }
        // As mentioned before, the file will only be downloaded after we collect the byte flow
        // Here, we start collecting the flow and print the rows affected by each bulk insert operation
        .collect(::println)

    // Hard to believe 100,000 items were inserted so quickly, isn't it?
    // To confirm this, I added a select statement so you can check for yourself:
    val count = jdbc.query("select count(1) from numbers").single()
    println("The count is $count")
}
