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
 *
 */
package net.ccbluex.liquidbounce.web.socket.protocol

import com.google.gson.*
import net.ccbluex.liquidbounce.config.ConfigSystem.registerCommonTypeAdapters
import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.config.adapter.ProtocolConfigurableSerializer
import net.minecraft.client.network.ServerInfo
import java.lang.reflect.Type
import java.util.*

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class ProtocolExclude

class ProtocolExclusionStrategy : ExclusionStrategy {
    override fun shouldSkipClass(clazz: Class<*>?) = false
    override fun shouldSkipField(field: FieldAttributes) = field.getAnnotation(ProtocolExclude::class.java) != null
}

class ServerInfoSerializer : JsonSerializer<ServerInfo> {
    override fun serialize(src: ServerInfo?, typeOfSrc: Type?, context: JsonSerializationContext?)
        = src?.asJsonObject()

    fun ServerInfo.asJsonObject() = JsonObject().apply {
        addProperty("name", name)
        addProperty("address", address)
        addProperty("online", online)
        add("playerList", protocolGson.toJsonTree(playerListSummary))
        add("label", protocolGson.toJsonTree(label))
        add("playerCountLabel", protocolGson.toJsonTree(playerCountLabel))
        add("version", protocolGson.toJsonTree(version))
        addProperty("protocolVersion", protocolVersion)
        add("players", JsonObject().apply {
            addProperty("max", players?.max)
            addProperty("online", players?.online)
        })

        favicon?.let {
            addProperty("icon", Base64.getEncoder().encodeToString(it))
        }
    }

}

internal val protocolGson = GsonBuilder()
    .addSerializationExclusionStrategy(ProtocolExclusionStrategy())
    .registerCommonTypeAdapters()
    .registerTypeHierarchyAdapter(Configurable::class.javaObjectType, ProtocolConfigurableSerializer)
    .registerTypeAdapter(ServerInfo::class.java, ServerInfoSerializer())
    .create()

