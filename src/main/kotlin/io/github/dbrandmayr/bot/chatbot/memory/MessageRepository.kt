package io.github.dbrandmayr.bot.chatbot.memory

import dev.kord.common.entity.Snowflake
import io.github.dbrandmayr.bot.Config
import io.github.dbrandmayr.bot.chatbot.ChatBotMessage
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.LocalDateTime

fun persistMessage(guildId: Snowflake, message: ChatBotMessage) {
    try {
        transaction {
            MessagesTable.insert {
                it[this.guildId] = guildId.toString()
                it[role] = message.role
                it[content] = message.content
                it[timestamp] = LocalDateTime.now()
            }
        }
    } catch (e: Exception) {
        println("Failed to persist message to DB: ${e.message}")
    }
}

fun loadAllChatHistories(): Map<Snowflake, MutableList<ChatBotMessage>> {
    return try {
        transaction {
            val guildIds = MessagesTable.selectAll()
                .map { it[MessagesTable.guildId] }
                .distinct()

            guildIds.associate { guildIdStr ->
                val messages = MessagesTable
                    .selectAll()
                    .where { MessagesTable.guildId eq guildIdStr }
                    .orderBy(MessagesTable.id, SortOrder.DESC)
                    .limit(Config.instance.bot.maxChatHistoryLength)
                    .toList()
                    .reversed()
                    .map { ChatBotMessage(role = it[MessagesTable.role], content = it[MessagesTable.content]) }

                Snowflake(guildIdStr) to messages.toMutableList()
            }
        }
    } catch (e: Exception) {
        println("Failed to load chat histories from DB: ${e.message}")
        emptyMap()
    }
}