# river

Kotlin Flows are amazing for building reactive pipelines, and, inspired by Apache Camel & Alpakka, this library aims to provide a toolkit for enterprise integrations that work on top of it.

#### Disclaimer: This project still is heavily under development, until a final release, anything is subject to change.

## Dependencies

Multi-platform is desired for a future release, but initially all the dependencies are JVM specific.

```kotlin
val latestVersion = "1.0.0-alpha"

//Core connector
implementation("io.github.gabfssilva.river:core:$latestVersion")
implementation("io.github.gabfssilva.river:format-json:$latestVersion")
implementation("io.github.gabfssilva.river:format-csv:$latestVersion")
implementation("io.github.gabfssilva.river:file:$latestVersion")

//AMQP
implementation("io.github.gabfssilva.river:amqp:$latestVersion")

//AWS
implementation("io.github.gabfssilva.river:aws-sqs:$latestVersion")
implementation("io.github.gabfssilva.river:aws-sns:$latestVersion")
implementation("io.github.gabfssilva.river:aws-s3:$latestVersion")
implementation("io.github.gabfssilva.river:aws-ses:$latestVersion")
implementation("io.github.gabfssilva.river:aws-dynamodb:$latestVersion")
implementation("io.github.gabfssilva.river:aws-lambda:$latestVersion")
implementation("io.github.gabfssilva.river:aws-kinesis:$latestVersion")

//RDBMS
implementation("io.github.gabfssilva.river:rdbms-jdbc:$latestVersion")
implementation("io.github.gabfssilva.river:rdbms-r2dbc:$latestVersion")

//Azure
implementation("io.github.gabfssilva.river:azure-queue-storage:$latestVersion")

//Console
implementation("io.github.gabfssilva.river:console:$latestVersion")

//ElasticSearch
implementation("io.github.gabfssilva.river:elasticsearch:$latestVersion")

implementation("io.github.gabfssilva.river:ftp:$latestVersion")

implementation("io.github.gabfssilva.river:jms:$latestVersion")

implementation("io.github.gabfssilva.river:mongodb:$latestVersion")

implementation("io.github.gabfssilva.river:mqtt:$latestVersion")



implementation("io.github.gabfssilva.river:twilio:$latestVersion")
```

