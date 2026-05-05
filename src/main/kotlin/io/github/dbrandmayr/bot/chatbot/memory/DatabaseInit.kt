package io.github.dbrandmayr.bot.chatbot.memory

import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

fun initDatabase(path: String = "botMemory.db") {
    Database.connect("jdbc:sqlite:$path", driver = "org.sqlite.JDBC")
    transaction {
        SchemaUtils.create(MessagesTable)
    }
}