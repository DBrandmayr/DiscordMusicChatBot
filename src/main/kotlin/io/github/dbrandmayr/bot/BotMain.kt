package io.github.dbrandmayr.bot

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import dev.schlaubi.lavakord.kord.lavakord
import io.github.dbrandmayr.bot.chatbot.ChatBotMessage
import io.github.dbrandmayr.bot.chatbot.ChatGptClient
import io.github.dbrandmayr.bot.chatbot.search.SearxngClient
import io.github.dbrandmayr.bot.chatbot.handleChatRequest
import io.github.dbrandmayr.bot.chatbot.memory.initDatabase
import io.github.dbrandmayr.bot.chatbot.memory.loadAllChatHistories
import io.github.dbrandmayr.bot.chatbot.memory.persistMessage
import io.github.dbrandmayr.bot.musicbot.*

lateinit var chatClient: ChatGptClient private set
lateinit var searxngClient: SearxngClient private set
lateinit var botSystemPrompt: String private set
lateinit var prefixes: Set<String> private set

val guildChatHistories = mutableMapOf<Snowflake, MutableList<ChatBotMessage>>()

lateinit var commands: List<Command> private set

suspend fun main(args: Array<String>) {
    val configPath = args.firstOrNull() ?: "config.yml"
    val config = Config.load(configPath)
    val musicEnabled = config.music.enabled
    commands = funCommands +
        (if (musicEnabled) musicCommands + queueCommands else emptyList()) +
        listOf(HelpCommand)
    val duplicates = findDuplicateCommandNames(commands)
    if (duplicates.isNotEmpty()) println("[33mWarning: duplicate command names detected: ${duplicates.joinToString(", ")}[0m")
    Messages.load(args.getOrNull(1) ?: "messages.yml")
    chatClient = ChatGptClient(config.chatbot.openai.key)
    searxngClient = SearxngClient()
    botSystemPrompt = config.chatbot.systemPrompt
    prefixes = config.bot.prefixes

    initDatabase()
    guildChatHistories.putAll(loadAllChatHistories())
    println("Chat histories loaded from database.")

    val kord = Kord(config.bot.token)
    LavalinkManager.initialize(kord.lavakord())
    LavalinkManager.connect(
        host = config.music.lavalink.host,
        port = config.music.lavalink.port,
        password = config.music.lavalink.password,
        secure = config.music.lavalink.secure
    )

    kord.on<ReadyEvent> {
        println("[1;32m══════════════════════════")
        println(" Bot is online!".padStart(20, ' '))
        println("══════════════════════════[0m")
    }

    kord.on<MessageCreateEvent> {
        val channel = message.channel

        if (message.author?.isBot != false) return@on

        val messageContent = message.content

        val prefix = prefixes.sortedByDescending { it.length }.find { messageContent.startsWith(it, ignoreCase = true) } ?: return@on

        val message = messageContent.substring(prefix.length).trimStart() // removes the prefix

        if (message.isBlank()) {
            channel.createMessage(Messages.instance.general.prompt)
            return@on
        }
        val messageWords = message.trim().split("\\s+".toRegex())
        val commandName = messageWords[0].lowercase()
        val args = messageWords.drop(1)
        val command = commands.find { it.names.contains(commandName) }

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

fun getChatHistory(guildId: Snowflake): List<ChatBotMessage> {
    return guildChatHistories[guildId]?.toList() ?: emptyList()
}

private fun findDuplicateCommandNames(commands: List<Command>): Set<String> {
    val seen = mutableSetOf<String>()
    val duplicates = mutableSetOf<String>()
    for (com in commands) {
        for (name in com.names) {
            if (!seen.add(name)) duplicates.add(name)
        }
    }
    return duplicates
}

fun addToChatHistory(guildId: Snowflake, message: ChatBotMessage): Boolean {
    val chatHistory = guildChatHistories.getOrPut(guildId) { mutableListOf() }
    if (chatHistory.size >= Config.instance.bot.maxChatHistoryLength) chatHistory.removeFirst()
    val added = chatHistory.add(message)
    persistMessage(guildId, message)
    return added
}