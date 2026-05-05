package io.github.dbrandmayr.bot

import io.github.dbrandmayr.bot.chatbot.ChatBotMessage
import io.github.dbrandmayr.bot.chatbot.ChatGptClient
import io.github.dbrandmayr.bot.chatbot.handleChatRequest
import io.github.dbrandmayr.bot.chatbot.memory.initDatabase
import io.github.dbrandmayr.bot.chatbot.memory.loadAllChatHistories
import io.github.dbrandmayr.bot.chatbot.memory.persistMessage
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import dev.schlaubi.lavakord.LavaKord
import dev.schlaubi.lavakord.kord.lavakord
import dev.schlaubi.lavakord.kord.getLink
import io.github.dbrandmayr.bot.musicbot.Command
import io.github.dbrandmayr.bot.musicbot.GuildMusicManager
import io.github.dbrandmayr.bot.musicbot.HelpCommand
import io.github.dbrandmayr.bot.musicbot.funCommands
import io.github.dbrandmayr.bot.musicbot.musicCommands
import io.github.dbrandmayr.bot.musicbot.queueCommands
import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

lateinit var lavalink: LavaKord private set
lateinit var chatClient: ChatGptClient private set
lateinit var botSystemPrompt: String private set
lateinit var prefixes: Set<String> private set

val guildMusicManagers = mutableMapOf<Snowflake, GuildMusicManager>()
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
    lavalink = kord.lavakord()

    connectToLavalinkWithRetry(
        lavalink,
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

private fun connectToLavalinkWithRetry(lavalink: LavaKord, host: String, port: Int, password: String) {
    CoroutineScope(Dispatchers.IO).launch {
        repeat(15) { attempt ->
            try {
                Socket().use { it.connect(InetSocketAddress(host, port), 1000) }
                lavalink.addNode("ws://$host:$port", password)
                println("Connected to Lavalink.")
                return@launch
            } catch (_: ConnectException) {
                val waitSeconds = (attempt + 1) * 4
                println("Lavalink not ready, retrying in ${waitSeconds}s... (${attempt + 1}/15)")
                delay(waitSeconds.seconds)
            }
        }
        println("Could not connect to Lavalink after 15 attempts.")
    }
}

fun getMusicManager(guildId: Snowflake): GuildMusicManager {
    val link = lavalink.getLink(guildId)
    return guildMusicManagers.getOrPut(guildId) {
        GuildMusicManager(link)
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