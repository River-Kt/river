# river

Introducing River, a powerful and flexible reactive stream library for Kotlin that simplifies the process of building connectors for multiple enterprise protocols and tools. Heavily inspired by Apache Camel and Alpakka, River makes use of Kotlin's Flow and coroutines to provide a scalable, efficient, and user-friendly way to handle asynchronous and event-based data streams.

With River, you can make use of connectors for a variety of enterprise protocols and tools, including message brokers, databases, cloud services, and more. This library is designed to be flexible and customizable, allowing you to handle complex data streams and integrate with different technologies seamlessly.

Whether you're building a new application or integrating with existing systems, River makes it easy to build reactive and scalable data pipelines that can handle even the most demanding workloads. With its powerful capabilities and easy-to-use API, River allows you to focus on your business logic while it'll take care of the complex task of handling data streams and integrating with multiple technologies.

## Talking is cheap, just show me the code!

Let's build something that listens to an Azure Queue and saves the messages into a PostgreSQL database using JDBC:

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

This code snippet a nutshell:

- A queue client is built using the QueueClientBuilder which specifies the name of the queue.
- A Jdbc object is created to estabilish connections with a PostgreSQL database with the provided credentials and a pool size of 10.
- Messages are received from the queue as a flow with a maximum of 10 parallelism.
- These messages are then batched together and inserted into the PostgreSQL database using a specified SQL query and message transformation function.
- The chunkStrategy parameter is set to a TimeWindow which groups messages into chunks based on their receipt time and flushes them to the database every 100 elements or when the timeout reaches 250 milliseconds, whichever comes first.

#### Disclaimer: This project is heavily under development, anything is subject to change until the first final release.



## License
This project is licensed under the MIT License - see the LICENSE.md file for details
