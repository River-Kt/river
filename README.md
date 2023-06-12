# river

Introducing River, a powerful and flexible reactive stream library for Kotlin that simplifies the process of using and building connectors for multiple enterprise protocols and tools. Heavily inspired by Apache Camel and Alpakka, River makes use of Kotlin's Flow and coroutines to provide a scalable, efficient, and user-friendly way to handle asynchronous and event-based data streams. This library is designed to be flexible and customizable, allowing you to handle complex data streams and integrate with different technologies seamlessly.

Whether you're building a new application or integrating with existing systems, River makes it easy to build reactive and scalable data pipelines that can handle even the most demanding workloads. With its powerful capabilities and easy-to-use API, River allows you to focus on your business logic while it'll take care of the complex task of handling data streams and integrating with multiple technologies.

#### Disclaimer: This project is heavily under development, anything is subject to change until the first final release.

## The core module

Kotlin's Flow API is a powerful and flexible tool for processing data streams, but it can sometimes require a lot of boilerplate code for certain tasks. That's where **River-Kt's core module** comes in. It provides a set of higher-level abstractions that make working with flows more intuitive and efficient.

At the heart of every **connector**, the **core module** offers a range of extension functions that can optimize the processing of data streams. By leveraging these high-level functions, developers can easily build connectors for different protocols. This allows for seamless integration with a variety of services and protocols.

The `mapAsync` is just an example of how the core module can simplify and speed up data processing by concurrently applying transformations to flow elements. Other functions such as `split` and `chunked` can also be used to break down large flows into smaller, more manageable chunks or groups, based on a count or time window strategy. Other useful functions are also provided, such as collecting data into lists, applying timeouts and delays, and continuously polling for data. All of these functions leverage Kotlin's Flow API, which ensures that data processing is done in an asynchronous, non-blocking, and backpressure-safe way.

Check the following example:

```kotlin
suspend fun fetchUsers(page: Int): List<User> = ...

// pollWithState allows polling for data continuously while maintaining a state, which makes it straightforward to handle halts and perform aggregations.
// The shouldStop predicate checks if the polling should be stopped before every poll request.
// In this case, we're stopping when the page is -1, which means we have already paginated the entire API.
// We're using the page number as the state to control which page to call next.
val users: Flow<User> = pollWithState(initial = 1, shouldStop = { it == -1 }) { page ->
    val usersPage = fetchUsers(page)
    
    val next = 
        if (usersPage.isEmpty()) -1 // no more pages available
        else page + 1 // maybe there's one page more
        
    next to usersPage
}

suspend fun fetchAddresses(userId: Long): List<Address> = ...

users
    // Runs the map function concurrently with a maximum of 10 concurrent executions.
    .mapAsync(10) { user -> UserWithAddresses(user, fetchAddresses(user.id)) }
    // Chunks the UserWithAddresses into lists of 100, or whatever fits into the chunk after the defined timeout of 1 second.
    .chunked(100, 1.seconds) 
    .collect { chunk: List<UserWithAddresses> ->
       // Do some processing with the chunk of UserWithAddresses...
    }
    
// Since it's still Kotlin's Flow, everything leverages asynchronous non-blocking backpressure:
// if the processing in the collect block is slow, the polling stage will slow down accordingly.
``` 

To get to know the core module a bit further, you can refer to the [site's kdoc](https://www.river-kt.com/core/com.river.core/) or you can [check the code](https://github.com/River-Kt/river/tree/main/core/src/main/kotlin/com/river/core) directly.

To start using it, simply add the following dependencies:

```kotlin
val riverVersion = "0.0.2-alpha01"
val coroutinesVersion = "1.7.1"

implementation("com.river-kt:core:$riverVersion")
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
```

## The connectors

Each connector leverages Kotlin's Flow API, coroutines and the core module to provide a simple and efficient way to interact with various protocols and services. You can check it out all the modules implemented currently.

| Connector Name                   | Description                                                                                                        | Module           |
|----------------------------------|--------------------------------------------------------------------------------------------------------------------|------------------|
| Amazon Simple Storage Service (Amazon S3)  | Allows developers to interact with Amazon S3 buckets via Kotlin's Flow API. It provides high-level functions to download and upload S3 objects.     | connector-aws-s3 |
| Amazon Simple Queue Service (Amazon SQS)   | Provides functionality to interact with Amazon SQS using Kotlin's Flow API. Developers can continuously receive, send, and delete messages from Amazon SQS using configurable chunk strategies and concurrency. | connector-aws-sqs |
| Amazon Simple Notification Service (Amazon SNS) | Allows users to send messages through Amazon SNS using Kotlin's Flow API using configurable chunk strategies and concurrency. | connector-aws-sns |
| AWS Lambda                        | Provides functionality to interact with AWS Lambda using Kotlin's Flow API. Users can invoke AWS Lambda functions and receive the results through a Flow stream using configurable concurrency.                    | connector-aws-lambda |
| Java Database Connectivity (JDBC)  | Provides an implementation of the RDBMS connector using JDBC driver. It allows users to interact with JDBC-compatible databases through Kotlin's Flow API using configurable chunk strategies and concurrency.                                 | connector-rdbms-jdbc |
| Reactive Relational Database Connectivity (R2DBC) | Provides an implementation of the RDBMS connector using R2DBC driver. It allows users to interact with R2DBC-compatible databases through Kotlin's Flow API using configurable chunk strategies and concurrency.                           | connector-rdbms-r2dbc |
| Debezium                          | Allows users to capture database events and stream them via Kotlin's Flow API. It supports multiple databases, including MySQL, PostgreSQL, and MongoDB. The emitted records can be sent to any other connector available in a seamless manner, enabling easy integration with a variety of services and protocols.                             | connector-redhat-debezium |
| Azure Queue Storage               | Provides functionality to interact with Azure Queue Storage using Kotlin's Flow API. Developers can continuously receive, create, update, and delete messages using configurable chunk strategies and concurrency. | connector-azure-queue-storage |
| JSON                              | Provides functionality to read and write JSON data using Kotlin's Flow API. It leverages Jackson, supports JSON serialization and deserialization with configurable options.                                | connector-format-json |
| CSV                               | Provides functionality to read and write CSV data using Kotlin's Flow API.                                             | connector-format-csv |
| File                              | Allows users to read and write files using Kotlin's Flow API. It provides functionality to read or write data to files and input streams.                  | connector-file    |

To install a module, you can add the dependency as follows:


```kotlin
/**
* Can be any of: 
*  [
*   connector-aws-s3, connector-aws-sqs, connector-aws-sns, connector-aws-lambda, 
*   connector-rdbms-jdbc, connector-rdbms-r2dbc, connector-redhat-debezium, 
*   connector-azure-queue-storage, connector-format-json, connector-format-csv, 
*   connector-file
*  ]
*/
val module = "connector-aws-sqs"

implementation("com.river-kt:$connectorName:$riverVersion")

```

## Talking is cheap, show me the code!

<details>
    <summary>Azure queue storage to PostgreSQL via JDBC</summary>

<br/>
The following example demonstrates how to transfer data from Azure Queue Storage to a PostgreSQL database using JDBC with River, using non-blocking execution, quick queue fetching, batched database inserts, and balanced resource utilization, achieving optimal speed with minimal overhead:


```kotlin
val queue = 
    QueueClientBuilder()
        .queueName("numbers")
        .buildAsyncClient()

val jdbc = Jdbc(
    url = "jdbc:postgresql://...",
    credentials = "xxx" to "xxx",
    connectionPoolSize = 10
)

val messages = queue.receiveMessagesAsFlow(concurrency = increaseByOne(10))

jdbc
    .batchUpdate(
        sql = "insert into numbers (number) values (?)",
        chunkStrategy = TimeWindow(100, 250.milliseconds),
        upstream = messages
    ) { message -> setString(1, message.messageText.toInt()) }
```

In a nutshell:

- A queue client is created using the `QueueClientBuilder`, which specifies the name of the queue as `numbers`.
- A `Jdbc` object is instantiated to establish connections with a `PostgreSQL` database using the provided credentials and a connection pool size of 10.
- Messages are received from the queue as a `Flow` using `queue.receiveMessagesAsFlow()` with a maximum concurrency of 10.
- The received messages are then `chunked` into groups of 100 messages or within 250 milliseconds, whichever comes first, using the `TimeWindow` strategy.
- After each chunk is emitted, the messages are batch-inserted into the `PostgreSQL` database using the `jdbc.batchUpdate()` function. The messages are inserted into the `numbers` table, with each message's text being converted to an integer and set as the value in the `number` column.

</details>

We will be adding more examples here soon.

## Contributing

Do you want to help us? Please, read our [Contributing Guidelines](CONTRIBUTING.md). Don't forget to read our [Code of conduct](CODE_OF_CONDUCT.md) as well.

## License
This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details
