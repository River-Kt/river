package com.river.connector.twilio

import com.river.connector.twilio.model.CreateMessage
import com.river.connector.twilio.model.Message
import com.river.core.mapAsync
import kotlinx.coroutines.flow.Flow

fun TwilioMessageHttpApi.sendMessageFlow(
    upstream: Flow<CreateMessage>,
    parallelism: Int = 1
): Flow<Message> = upstream.mapAsync(parallelism) { createMessage(it) }
