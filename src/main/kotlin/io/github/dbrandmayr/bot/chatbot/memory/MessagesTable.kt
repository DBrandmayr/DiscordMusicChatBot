package io.github.dbrandmayr.bot.chatbot.memory

import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.javatime.datetime

object MessagesTable : IntIdTable("messages") {
    val guildId = varchar("guild_id", 64)
    val role = varchar("role", 16)
    val content = text("content")
    val timestamp = datetime("timestamp")
}