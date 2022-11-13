package io.github.gabfssilva.river.twilio

import io.github.gabfssilva.river.core.mapParallel
import kotlinx.coroutines.flow.Flow

context(Flow<CreateMessage>)
fun TwilioMessageHttpApi.sendMessageFlow(
    parallelism: Int = 1
): Flow<Message> = mapParallel(parallelism) { createMessage(it) }

fun TwilioMessageHttpApi.sendMessageFlow(
    upstream: Flow<CreateMessage>,
    parallelism: Int = 1
): Flow<Message> = with(upstream) { sendMessageFlow(parallelism) }
