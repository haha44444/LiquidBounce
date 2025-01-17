/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.features.misc

import io.netty.channel.ChannelPipeline
import io.netty.handler.proxy.Socks5ProxyHandler
import net.ccbluex.liquidbounce.api.IpInfoApi
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.config.ListValueType
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.events.PipelineEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.misc.ProxyManager.Proxy.Companion.NO_PROXY
import net.ccbluex.liquidbounce.features.misc.ProxyManager.ProxyCredentials.Companion.credentialsFromUserInput
import java.net.InetSocketAddress

/**
 * Proxy Manager
 *
 * Only supports SOCKS5 proxies.
 */
object ProxyManager : Configurable("proxy"), Listenable {

    var proxy by value("proxy", NO_PROXY)
    val proxies by value(name, mutableListOf<Proxy>(), listType = ListValueType.Proxy)

    /**
     * The proxy that is set in the current session and used for all server connections
     */
    val currentProxy
        get() = proxy.takeIf { it.host.isNotBlank() }

    init {
        ConfigSystem.root(this)
    }

    fun addProxy(host: String, port: Int, username: String = "", password: String = "") {
        proxies.add(Proxy(host, port, credentialsFromUserInput(username, password)))
        ConfigSystem.storeConfigurable(this)
    }

    fun removeProxy(index: Int) {
        proxies.removeAt(index)
        ConfigSystem.storeConfigurable(this)
    }

    fun setProxy(index: Int) {
        proxy = proxies[index]
        sync()
    }

    fun unsetProxy() {
        proxy = NO_PROXY
        sync()
    }

    private fun sync() {
        ConfigSystem.storeConfigurable(this)
        IpInfoApi.refreshLocalIpInfo()
    }

    /**
     * Adds a SOCKS5 netty proxy handler to the pipeline when a proxy is set
     *
     * @see Socks5ProxyHandler
     * @see PipelineEvent
     */
    val pipelineHandler = handler<PipelineEvent> {
        val pipeline = it.channelPipeline
        insertProxyHandler(pipeline)
    }

    fun insertProxyHandler(pipeline: ChannelPipeline) {
        currentProxy?.run {
            pipeline.addFirst(
                "proxy",
                if (credentials != null) {
                    Socks5ProxyHandler(address, credentials.username, credentials.password)
                } else {
                    Socks5ProxyHandler(address)
                }
            )
        }
    }

    /**
     * Contains serializable proxy data
     */
    data class Proxy(val host: String, val port: Int, val credentials: ProxyCredentials?) {
        val address
            get() = InetSocketAddress(host, port)

        companion object {
            val NO_PROXY = Proxy("", 0, null)
        }

    }

    /**
     * Contains serializable proxy credentials
     */
    data class ProxyCredentials(val username: String, val password: String) {
        companion object {
            fun credentialsFromUserInput(username: String, password: String) =
                if (username.isNotBlank() && password.isNotBlank())
                    ProxyCredentials(username, password)
                else
                    null
        }
    }

}
