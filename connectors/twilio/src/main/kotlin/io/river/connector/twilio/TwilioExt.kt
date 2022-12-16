package io.river.connector.twilio

import io.river.connector.twilio.model.CreateMessage
import io.river.connector.twilio.model.Message
import io.river.core.mapParallel
import kotlinx.coroutines.flow.Flow

fun TwilioMessageHttpApi.sendMessageFlow(
    upstream: Flow<CreateMessage>,
    parallelism: Int = 1
): Flow<Message> = upstream.mapParallel(parallelism) { createMessage(it) }
