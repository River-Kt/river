# river

Introducing River, a powerful and flexible reactive stream library for Kotlin that simplifies the process of using and building connectors for multiple enterprise protocols and tools. Heavily inspired by Apache Camel and Alpakka, River makes use of Kotlin's Flow and coroutines to provide a scalable, efficient, and user-friendly way to handle asynchronous and event-based data streams.

With River, you can make use of connectors for a variety of enterprise protocols and tools, including message brokers, databases, cloud services, and more. This library is designed to be flexible and customizable, allowing you to handle complex data streams and integrate with different technologies seamlessly.

Whether you're building a new application or integrating with existing systems, River makes it easy to build reactive and scalable data pipelines that can handle even the most demanding workloads. With its powerful capabilities and easy-to-use API, River allows you to focus on your business logic while it'll take care of the complex task of handling data streams and integrating with multiple technologies.

#### Disclaimer: This project is heavily under development, anything is subject to change until the first final release.

## Getting started

In order to start using River, you have to install the desired dependencies:

```kotlin
val riverVersion = "0.0.1-alpha01"
val coroutinesVersion = "1.6.4"

dependencies {
    // Mandatory libraries
    implementation("com.river-kt:core:$riverVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk9:$coroutinesVersion")

    // Utilities for Kotlin Flow and HTTP
    implementation("com.river-kt:http:$riverVersion")
    
    // Utilities for object pooling using coroutines
    implementation("com.river-kt:pool:$riverVersion")

    // Add the desired connectors (some of them are not implemented yet, be patient. ;)
    implementation("com.river-kt:connector-amqp:$riverVersion")
    implementation("com.river-kt:connector-apache-kafka:$riverVersion")
    
    implementation("com.river-kt:connector-aws-dynamodb:$riverVersion")
    implementation("com.river-kt:connector-aws-java-11-http-spi:$riverVersion")
    implementation("com.river-kt:connector-aws-kinesis:$riverVersion")
    implementation("com.river-kt:connector-aws-lambda:$riverVersion")
    implementation("com.river-kt:connector-aws-s3:$riverVersion")
    implementation("com.river-kt:connector-aws-ses:$riverVersion")
    implementation("com.river-kt:connector-aws-sns:$riverVersion")
    implementation("com.river-kt:connector-aws-sqs:$riverVersion")
    
    implementation("com.river-kt:connector-azure-queue-storage:$riverVersion")
    
    implementation("com.river-kt:connector-console:$riverVersion")
    
    implementation("com.river-kt:connector-elasticsearch:$riverVersion")
    
    implementation("com.river-kt:connector-file:$riverVersion")
    
    implementation("com.river-kt:connector-format-csv:$riverVersion")
    implementation("com.river-kt:connector-format-json:$riverVersion")
    
    implementation("com.river-kt:connector-ftp:$riverVersion")
    
    implementation("com.river-kt:connector-jms:$riverVersion")
    
    implementation("com.river-kt:connector-mongodb:$riverVersion")
    
    implementation("com.river-kt:connector-mqtt:$riverVersion")
    
    implementation("com.river-kt:connector-rdbms-jdbc:$riverVersion")
    implementation("com.river-kt:connector-rdbms-r2dbc:$riverVersion")
    
    implementation("com.river-kt:connector-red-hat-debezium:$riverVersion")
    
    implementation("com.river-kt:connector-twilio:$riverVersion")
}
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

val messages = queue.receiveMessagesAsFlow(maxParallelism = 10)

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
- Messages are received from the queue as a `Flow` using `queue.receiveMessagesAsFlow()` with a maximum parallelism of 10.
- The received messages are then `chunked` into groups of 100 messages or within 250 milliseconds, whichever comes first, using the `TimeWindow` strategy.
- After each chunk is emitted, the messages are batch-inserted into the `PostgreSQL` database using the `jdbc.batchUpdate()` function. The messages are inserted into the `numbers` table, with each message's text being converted to an integer and set as the value in the `number` column.

</details>

## License
This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details
