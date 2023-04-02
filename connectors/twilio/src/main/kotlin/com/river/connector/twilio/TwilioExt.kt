package com.river.connector.twilio

import com.river.connector.twilio.model.CreateMessage
import com.river.connector.twilio.model.Message
import com.river.core.mapParallel
import kotlinx.coroutines.flow.Flow

fun TwilioMessageHttpApi.sendMessageFlow(
    upstream: Flow<CreateMessage>,
    parallelism: Int = 1
): Flow<Message> = upstream.mapParallel(parallelism) { createMessage(it) }
