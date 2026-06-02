package io.github.dbrandmayr.bot.chatbot

import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Message
import dev.kord.core.event.message.MessageCreateEvent
import io.github.dbrandmayr.bot.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.URI
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.time.toJavaInstant


suspend fun handleChatRequest(event: MessageCreateEvent){
    val channel = event.message.channel
    val message = event.message

    val guildId = event.guildId ?: run {
        println("Returned at getting guildId while handling chat request")
        return
    }

    val useBase64 = Config.instance.chatbot.openai.useBase64Images
    val imageUrls = message.attachments
        .filter { it.contentType?.startsWith("image/") == true }
        .map { attachment ->
            if (useBase64) resolveImageFromUrl(attachment.url) else attachment.url
        }
    val processImages = imageUrls.isNotEmpty() && Config.instance.chatbot.allowImages

    val systemMessage = getSystemMessage(botSystemPrompt, Config.instance.music.enabled, processImages)
    val userMessage = formatMessage(message) ?: return
    val chatHistory = getChatHistory(guildId)

    val userContent: Any = if (!processImages) {
        userMessage
    } else {
        buildList {
            add(TextPart(text = userMessage))
            imageUrls.forEach { add(ImageUrlPart(imageUrl = mapOf("url" to it))) }
        }
    }

    val queryMessages = buildList {
        add(ApiMessage(role = "system", content = systemMessage))
        addAll(chatHistory.map { ApiMessage(it.role, it.content) })
        add(ApiMessage(role = "user", content = userContent))
    }

    try {
        val response = chatClient.sendMessage(queryMessages)
        val (convResponse, imageDescription) = resolveAndExecuteResponseCommands(response, event)
        val storedUserMessage = if (imageDescription != null) "$userMessage\n[Image: $imageDescription]" else userMessage
        addToChatHistory(guildId, ChatBotMessage("user", storedUserMessage))
        addToChatHistory(guildId, ChatBotMessage("assistant", response))
        channel.createMessage(convResponse)
    } catch (e: Exception) {
        channel.createMessage("I wasn't able to respond to that. Please try again.")
        println("Chat API error: ${e.message}")
    }
}

private suspend fun resolveImageFromUrl(url: String): String {
    val bytes = withContext(Dispatchers.IO) { URI.create(url).toURL().readBytes() }
    val base64 = Base64.getEncoder().encodeToString(bytes)
    return "data:image/png;base64,$base64"
}

suspend fun resolveAndExecuteResponseCommands(response: String, event: MessageCreateEvent): Pair<String, String?> {
    val jsonRegex = """\{.*?}""".toRegex()
    val matches = jsonRegex.findAll(response).toList()
    if (matches.isEmpty()) return Pair(response, null)

    var result = response
    var imageDescription: String? = null

    for (match in matches) {
        val commandJson = match.value
        try {
            val botCommand = Json.decodeFromString<BotCommandClass>(commandJson)
            val commandResult = when (botCommand.command) {
                "describe_image" -> {
                    imageDescription = botCommand.args.firstOrNull()
                    null
                }
                else -> botCommands[botCommand.command]?.execute(botCommand.args, event)
            }
            result = result.replace(commandJson, commandResult ?: "")
        } catch (e: Exception) {
            println("Failed to parse Command JSON: ${e.message}")
        }
    }

    return Pair(result, imageDescription)
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

