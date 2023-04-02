package com.river.connector.jms.model

import javax.jms.JMSConsumer
import javax.jms.JMSContext

typealias JmsConsumer = Pair<JMSContext, JMSConsumer>
