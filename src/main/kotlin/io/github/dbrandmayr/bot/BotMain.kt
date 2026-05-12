package io.github.dbrandmayr.bot

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import dev.schlaubi.lavakord.kord.lavakord
import io.github.dbrandmayr.bot.chatbot.ChatBotMessage
import io.github.dbrandmayr.bot.chatbot.ChatGptClient
import io.github.dbrandmayr.bot.chatbot.handleChatRequest
import io.github.dbrandmayr.bot.chatbot.memory.initDatabase
import io.github.dbrandmayr.bot.chatbot.memory.loadAllChatHistories
import io.github.dbrandmayr.bot.chatbot.memory.persistMessage
import io.github.dbrandmayr.bot.musicbot.*


lateinit var chatClient: ChatGptClient private set
lateinit var botSystemPrompt: String private set
lateinit var prefixes: Set<String> private set

val guildChatHistories = mutableMapOf<Snowflake, MutableList<ChatBotMessage>>()

val commands: List<Command> = funCommands + musicCommands + queueCommands + listOf(HelpCommand)

suspend fun main(args: Array<String>) {
    val configPath = args.firstOrNull() ?: "config.yml"
    val config = Config.load(configPath)
    chatClient = ChatGptClient(config.openai.key)
    botSystemPrompt = config.chatbot.systemPrompt
    prefixes = config.bot.prefixes

    initDatabase()
    guildChatHistories.putAll(loadAllChatHistories())
    println("Chat histories loaded from database.")

    val kord = Kord(config.bot.token)
    LavalinkManager.initialize(kord.lavakord())
    LavalinkManager.connect(
        host = config.lavalink.host,
        port = config.lavalink.port,
        password = config.lavalink.password
    )


    kord.on<MessageCreateEvent> {
        val channel = message.channel

        if (message.author?.isBot != false) return@on

        val messageContent = message.content

        val prefix = prefixes.sortedByDescending { it.length }.find { messageContent.startsWith(it, ignoreCase = true) } ?: return@on

        val message = messageContent.substring(prefix.length).trimStart() // removes the prefix

        if (message.isBlank()) {
            channel.createMessage("What can I do for you?")
            return@on
        }
        val messageWords = message.trim().split("\\s+".toRegex())
        val commandName = messageWords[0].lowercase()
        val args = messageWords.drop(1)
        val command = commands.find { it.name == commandName || it.aliases.contains(commandName) }

        if (command != null){
            command.execute(args, this)
        } else if (config.chatbot.enabled){
            // Not a command -> ChatGPT should answer
            handleChatRequest(this)
        }
    }

    kord.login{
        // needs to be specified to be able to receive the content of messages
        @OptIn(PrivilegedIntent::class)
        intents += Intent.MessageContent
    }
}


fun getChatHistory(guildId: Snowflake): List<ChatBotMessage>? {
    return guildChatHistories[guildId]?.toList()
}

fun addToChatHistory(guildId: Snowflake, message: ChatBotMessage): Boolean {
    val chatHistory = guildChatHistories.getOrPut(guildId) { mutableListOf() }
    if (chatHistory.size >= Config.instance.bot.maxChatHistoryLength) chatHistory.removeFirst()
    val added = chatHistory.add(message)
    persistMessage(guildId, message)
    return added
}