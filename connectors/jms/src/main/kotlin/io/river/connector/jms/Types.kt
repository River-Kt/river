package io.river.connector.jms

import javax.jms.JMSConsumer
import javax.jms.JMSContext

typealias JmsConsumer = Pair<JMSContext, JMSConsumer>
