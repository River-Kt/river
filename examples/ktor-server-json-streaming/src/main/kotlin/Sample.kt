@file:OptIn(FlowPreview::class)

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.river.connector.format.json.defaultObjectMapper
import com.river.connector.format.json.parseJsonArray
import com.river.connector.http.*
import com.river.core.flatten
import com.river.core.indefinitelyRepeat
import com.river.core.unboundedLongFlow
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import java.net.http.HttpResponse
import java.util.*
import kotlin.time.Duration.Companion.seconds
import com.river.connector.http.get as riverGet

const val port = 8080

suspend fun main() {
    embeddedServer(Netty, port = port, module = Application::api)
        .start(wait = true)
}

data class Order(val amount: Int, val userId: UUID)

inline fun <reified T> ofJson(
    objectMapper: ObjectMapper = defaultObjectMapper
): HttpResponse.BodyHandler<T> =
    ofByteArray.map { objectMapper.readValue<T>(it) }

inline fun <reified T : Any> ofJsonStream(
    objectMapper: ObjectMapper = defaultObjectMapper
) = ofByteArrayFlow.map { it.parseJsonArray<T>(objectMapper) }

fun Application.api() {
    install(ContentNegotiation) {
        register(
            contentType = ContentType.Application.Json,
            converter = JacksonConverter(
                objectMapper = defaultObjectMapper,
                streamRequestBody = true
            )
        )
    }

    routing {
        // A simple route for fetching pages of orders
        get("/orders") {
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1

            // If page > 20, respond with an empty list, else generate a list of orders
            val response =
                if (page > 20) emptyList()
                else (1..100).map { Order(it, UUID.randomUUID()) }

            delay(500) // Simulating network delay, just to make things fun
            call.respond(response)
        }

        // Route for all the orders as stream
        get("/order-stream") {
            // Create a flow of all the order pages
            val orderFlow =
                // Generate sequential longs starting from 1
                unboundedLongFlow(1)
                    // Use each number as a page number for the HTTP request
                    .map { riverGet("http://localhost:$port/orders") { query("page", "$it") } }
                    // Send requests with parallelism of 2 for this example
                    .sendAndHandle(ofJson<List<Order>>(), 2)
                    .map { it.body() }
                    // Stop when an empty list is returned
                    .takeWhile { it.isNotEmpty() }
                    // Flatten the results into a single stream
                    .flatten()

            call.respond(orderFlow)
        }

        data class OrderSum(val count: Int, val sum: Int)

        // Route for fetching the sum of orders
        get("orders-sum") {
            // Initiate an HTTP request to fetch all the orders from the /order-stream endpoint.
            // At this point, only the HTTP headers are parsed, and the body (containing the actual order data)
            // awaits consumption. This is a key aspect of streaming: data is consumed on demand, not all at once.
            val orders: Flow<Order> =
                riverGet("http://localhost:$port/order-stream")
                    .coSend(ofJsonStream<Order>())
                    .body()

            // It's important to note that the process of pagination and data fetching will only begin once
            // we start collecting items from the 'orders' Flow. If the data consumption becomes slow for some reason,
            // so does the pagination process. This is backpressure in action and that's how it's ensured that the consumer
            // isn't overwhelmed by the producer generating data too quickly.

            call.respond(
                orders
                    .runningFold(0) { acc, order -> acc + order.amount } // Accumulating the sum of orders amount
                    .filterNot { it == 0 } // Skipping the initial value
                    .withIndex() // Adding an index to each sum
                    .map { (index, sum) -> OrderSum(index + 1, sum) } // Mapping each sum to an OrderSum object
                    // It emits samples at regular intervals of 1 second. This means that
                    // instead of emitting every calculated sum immediately, it will emit the latest sum every second.
                    // This is especially useful for real-time data analytics.
                    .sample(1.seconds)
            )
        }
    }
}
