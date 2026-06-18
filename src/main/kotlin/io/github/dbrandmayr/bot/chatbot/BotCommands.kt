package io.github.dbrandmayr.bot.chatbot

import dev.arbjerg.lavalink.protocol.v4.LoadResult
import dev.kord.core.event.message.MessageCreateEvent
import dev.schlaubi.lavakord.audio.Link
import dev.schlaubi.lavakord.rest.loadItem
import io.github.dbrandmayr.bot.Messages
import io.github.dbrandmayr.bot.fill
import io.github.dbrandmayr.bot.formatAddedTracks
import io.github.dbrandmayr.bot.musicbot.LavalinkManager
import io.github.dbrandmayr.bot.musicbot.getGuildId
import kotlinx.serialization.Serializable

@Serializable
data class BotCommandClass(
    val command: String,
    val args: List<String>
)

interface BotCommand{
    val name: String
    suspend fun execute(args: List<String>, event: MessageCreateEvent): String?
}

val botCommands: Map<String, BotCommand> = listOf(
    PlayBotCommand()
).associateBy { it.name }

class PlayBotCommand : BotCommand {
    override val name: String = "play"
    override suspend fun execute(args: List<String>, event: MessageCreateEvent) : String? {
        val channel = event.message.channel
        val guildId = getGuildId(event)
        val member = event.member ?: run {
            channel.createMessage(Messages.instance.common.notInSameChannel)
            return null
        }
        val voiceState = member.getVoiceStateOrNull()
        val voiceChannelId = voiceState?.channelId ?: run {
            channel.createMessage(Messages.instance.common.notInSameChannel)
            return null
        }
        val musicManager = LavalinkManager.getMusicManager(guildId)
        val link = musicManager.link
        if (link.state != Link.State.CONNECTED){
            link.connectAudio(voiceChannelId.value)
        }

        val addedTracks = mutableListOf<Pair<String, Long>>()

        args.forEach { title ->
            val searchQuery = "ytsearch:$title"
            when (val loadResult = link.loadItem(searchQuery)) {
                is LoadResult.TrackLoaded -> {
                    val startedNow = musicManager.playTrack(loadResult.data)
                    if (startedNow) {
                        musicManager.replayTrack = loadResult.data
                    }
                    addedTracks.add(loadResult.data.info.title to loadResult.data.info.length)
                }
                is LoadResult.PlaylistLoaded -> {
                    val firstTrack = loadResult.data.tracks.first()
                    val remainingTracks = loadResult.data.tracks.drop(1)
                    musicManager.playTrack(firstTrack)
                    musicManager.addToQueue(remainingTracks)
                    musicManager.replayTrack = firstTrack
                    loadResult.data.tracks.mapTo(addedTracks) { it.info.title to it.info.length }
                }
                is LoadResult.SearchResult -> {
                    val firstTrack = loadResult.data.tracks.first()
                    val startedNow = musicManager.playTrack(firstTrack)
                    if (startedNow) {
                        musicManager.replayTrack = firstTrack
                    }
                    addedTracks.add(firstTrack.info.title to firstTrack.info.length)
                }
                is LoadResult.LoadFailed -> channel.createMessage(Messages.instance.common.searchFailed)
                is LoadResult.NoMatches -> channel.createMessage(Messages.instance.common.searchNoResults.fill("query" to title))
            }
        }

        return if (addedTracks.isEmpty()) null else formatAddedTracks(addedTracks)
    }
}
