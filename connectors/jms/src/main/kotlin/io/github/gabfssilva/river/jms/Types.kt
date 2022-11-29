package io.github.gabfssilva.river.jms

import javax.jms.JMSConsumer
import javax.jms.JMSContext

typealias JmsConsumer = Pair<JMSContext, JMSConsumer>
