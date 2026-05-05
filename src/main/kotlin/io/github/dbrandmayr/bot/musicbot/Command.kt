package io.github.dbrandmayr.bot.musicbot

import io.github.dbrandmayr.bot.getMusicManager
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Guild
import dev.kord.core.event.message.MessageCreateEvent
import dev.schlaubi.lavakord.audio.Link
import dev.schlaubi.lavakord.audio.player.Player

interface Command {
    val name: String
    val aliases: List<String> get() = emptyList()
    val category: String
    val description: String
    suspend fun execute(args: List<String>, event: MessageCreateEvent)
}

suspend fun getGuild(event: MessageCreateEvent): Guild =
    event.getGuildOrNull() ?: throw IllegalStateException("Unable to retrieve the guild.")

suspend fun getGuildId(event: MessageCreateEvent): Snowflake = getGuild(event).id

suspend fun getPlayer(event: MessageCreateEvent): Player =
    getMusicManager(getGuildId(event)).player

suspend fun isUserInSameChannel(event: MessageCreateEvent, link: Link): Boolean {
    val userVoiceChannelId = event.member?.getVoiceStateOrNull()?.channelId ?: return false
    return userVoiceChannelId.value == link.lastChannelId
}