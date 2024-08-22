package com.river.connector.ktor.network

import com.river.connector.ktor.network.internal.ClientTcpExtensions
import com.river.connector.ktor.network.internal.ServerTcpExtensions

object Tcp : ServerTcpExtensions, ClientTcpExtensions
