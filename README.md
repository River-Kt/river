# river

Kotlin Flows are amazing for building reactive pipelines, and, inspired by Apache Camel & Alpakka, this library aims to provide a toolkit for enterprise integrations that work on top of it.

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

