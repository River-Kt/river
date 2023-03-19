# river

Introducing River, a powerful and flexible reactive stream library for Kotlin that simplifies the process of building connectors for multiple enterprise protocols and tools. Heavily inspired by Apache Camel and Alpakka, River makes use of Kotlin's Flow and coroutines to provide a scalable, efficient, and user-friendly way to handle asynchronous and event-based data streams.

With River, you can make use of connectors for a variety of enterprise protocols and tools, including message brokers, databases, cloud services, and more. Our library is designed to be flexible and customizable, allowing you to handle complex data streams and integrate with different technologies seamlessly.

Whether you're building a new application or integrating with existing systems, River makes it easy to build reactive and scalable data pipelines that can handle even the most demanding workloads. With its powerful capabilities and easy-to-use API, River allows you to focus on your business logic while we take care of the complex task of handling data streams and integrating with multiple technologies.

#### Disclaimer: This project still is heavily under development, until a final release, anything is subject to change.

## Dependencies

Multi-platform is desired for a future release, but initially all the dependencies are JVM specific.

```kotlin
val latestVersion = "1.0.0-alpha"

//Core connector
implementation("io.river:core:$latestVersion")
implementation("io.river.connector:format-json:$latestVersion")
implementation("io.river.connector:format-csv:$latestVersion")
implementation("io.river.connector:file:$latestVersion")

//AMQP
implementation("io.river.connector:amqp:$latestVersion")

//AWS
implementation("io.river.connector:aws-sqs:$latestVersion")
implementation("io.river.connector:aws-sns:$latestVersion")
implementation("io.river.connector:aws-s3:$latestVersion")
implementation("io.river.connector:aws-ses:$latestVersion")
implementation("io.river.connector:aws-dynamodb:$latestVersion")
implementation("io.river.connector:aws-lambda:$latestVersion")
implementation("io.river.connector:aws-kinesis:$latestVersion")

//RDBMS
implementation("io.river.connector:rdbms-jdbc:$latestVersion")
implementation("io.river.connector:rdbms-r2dbc:$latestVersion")

//Azure
implementation("io.river.connector:azure-queue-storage:$latestVersion")

//Console
implementation("io.river.connector:console:$latestVersion")

//ElasticSearch
implementation("io.river.connector:elasticsearch:$latestVersion")

implementation("io.river.connector:ftp:$latestVersion")

implementation("io.river.connector:jms:$latestVersion")

implementation("io.river.connector:mongodb:$latestVersion")

implementation("io.river.connector:mqtt:$latestVersion")



implementation("io.river.connector:twilio:$latestVersion")
```
