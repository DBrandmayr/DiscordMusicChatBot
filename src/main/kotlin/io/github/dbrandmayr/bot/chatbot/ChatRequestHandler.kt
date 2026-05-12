package io.github.dbrandmayr.bot.chatbot

import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Message
import dev.kord.core.event.message.MessageCreateEvent
import io.github.dbrandmayr.bot.*
import kotlinx.serialization.json.Json
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.toJavaInstant


suspend fun handleChatRequest(event: MessageCreateEvent){
    val channel = event.message.channel
    val message = event.message

    val guildId = event.guildId ?: run {
        println("Returned at getting guildId while handling chat request")
        return
    }

    val systemMessage = getSystemMessage(botSystemPrompt)


    val userMessage = formatMessage(message) ?: return


    if (!addToChatHistory(guildId, ChatBotMessage(role = "user", content = userMessage))) {
        println("Returned at adding message to history while handling chat request")
        return
    }


    val chatHistory = getChatHistory(guildId) ?: run {
        println("Returned at getting chat history while handling chat request")
        return
    }

    val queryMessages = mutableListOf(
        ChatBotMessage(role = "system", content = systemMessage),
    )
    queryMessages.addAll(chatHistory)

    try {
        val response = chatClient.sendMessage(queryMessages)
        addToChatHistory(guildId, ChatBotMessage("assistant", response))
        val convResponse = resolveAndExecuteResponseCommands(response, event)
        channel.createMessage(convResponse)
    } catch (e: Exception) {
        channel.createMessage("Something went wrong. Please try again.")
        println("Error with ChatGPT: ${e.message}")
    }
}

suspend fun resolveAndExecuteResponseCommands(answer: String, event: MessageCreateEvent): String{
    val jsonRegex = """\{.*}""".toRegex()
    val commandJson = jsonRegex.find(answer)?.value ?: return answer
    val convMessage = answer.replace(commandJson, "")
    try {
        val botCommand = Json.decodeFromString<BotCommandClass>(commandJson)
        when (botCommand.command) {
            "play" -> PlayBotCommand().execute(botCommand.args, event)
        }
    }catch (e: Exception){
        println("Failed to parse Command JSON: ${e.message}")
    }
    return convMessage
}

suspend fun resolveMentionNames(messageContent: String, guild: Guild): String {
    val userMentionRegex = "<@!?(\\d+)>".toRegex() // Matches user mentions
    val roleMentionRegex = "<@&(\\d+)>".toRegex() // Matches role mentions
    val channelMentionRegex = "<#(\\d+)>".toRegex() // Matches channel mentions
    val customEmojiRegex = "<a?:(\\w+):(\\d+)>".toRegex()

    var resolvedContent = messageContent

    // Resolve user mentions
    userMentionRegex.findAll(messageContent).forEach { match ->
        val userId = match.groupValues[1]
        val user = guild.getMemberOrNull(Snowflake(userId))?.effectiveName ?: "Unknown User"
        resolvedContent = resolvedContent.replace(match.value, "@$user")
    }

    // Resolve role mentions
    roleMentionRegex.findAll(messageContent).forEach { match ->
        val roleId = match.groupValues[1]
        val role = guild.getRoleOrNull(Snowflake(roleId))?.name ?: "Unknown Role"
        resolvedContent = resolvedContent.replace(match.value, "@$role")
    }

    // Resolve channel mentions
    channelMentionRegex.findAll(messageContent).forEach { match ->
        val channelId = match.groupValues[1]
        val channel = guild.getChannelOrNull(Snowflake(channelId))?.data?.name?.value ?: "Unknown Channel"
        resolvedContent = resolvedContent.replace(match.value, "#$channel", false)
    }

    // Resolve custom emoji mentions
    customEmojiRegex.findAll(messageContent).forEach { match ->
        resolvedContent = resolvedContent.replace(match.value, ":${match.groupValues[1]}:")
    }

    return resolvedContent
}

suspend fun formatMessage(message: Message):String? {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
    val time = timeFormatter.format(message.timestamp.toJavaInstant())
    val guild = message.getGuildOrNull() ?: run {
        println("Returned null at getting guild while formatting message")
        return null
    }
    var messageContent = resolveMentionNames(message.content, guild)
    val prefix = prefixes.sortedByDescending { it.length }.find { messageContent.startsWith(it, ignoreCase = true) } ?: run {
        println("Returned null at searching prefix while formatting message")
        return null
    }

    messageContent = messageContent.substring(prefix.length) // removes the prefix
    val author = message.getAuthorAsMember()

    val channel = guild.getChannelOrNull(message.channelId)?.data?.name?.value ?: "Unknown Channel"


    return "[${time}] ${author.effectiveName} (${author.globalName}) | #${channel}: $messageContent"
}

