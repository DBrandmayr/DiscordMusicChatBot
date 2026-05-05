package io.github.dbrandmayr.bot.chatbot

import io.github.dbrandmayr.bot.musicbot.Command
import io.github.dbrandmayr.bot.musicbot.getGuildId
import io.github.dbrandmayr.bot.getMusicManager
import dev.arbjerg.lavalink.protocol.v4.LoadResult
import dev.kord.core.event.message.MessageCreateEvent
import dev.schlaubi.lavakord.audio.Link
import dev.schlaubi.lavakord.rest.loadItem
import kotlinx.serialization.Serializable

@Serializable
data class BotCommandClass(
    val command: String,
    val args: List<String>
)



class PlayBotCommand : Command {
    override val name: String = "play"
    override val category: String = "internal"
    override val description: String = "internal"
    override suspend fun execute(args: List<String>, event: MessageCreateEvent) {
        val channel = event.message.channel
        val guildId = getGuildId(event)
        val member = event.member ?: run {
            channel.createMessage("You need to be in the same voice channel as the bot.")
            return
        }
        val voiceState = member.getVoiceStateOrNull()
        val voiceChannelId = voiceState?.channelId ?: run {
            channel.createMessage("You need to be in the same voice channel as the bot.")
            return
        }
        val musicManager = getMusicManager(guildId)
        val link = musicManager.link
        if (link.state != Link.State.CONNECTED){
            link.connectAudio(voiceChannelId.value)
        }
        args.forEach{title->
            val searchQuery = "ytsearch:$title"
            when (val loadResult = link.loadItem(searchQuery)){
                is LoadResult.TrackLoaded -> {
                    val startedNow =  musicManager.playTrack(loadResult.data)
                    if (startedNow){
                        musicManager.replayTrack = loadResult.data
                    }
                }
                is LoadResult.PlaylistLoaded -> {
                    val firstTrack = loadResult.data.tracks.first()
                    val remainingTracks = loadResult.data.tracks.drop(1)

                    musicManager.playTrack(firstTrack)
                    musicManager.trackQueue.addAll(remainingTracks)
                    musicManager.replayTrack = firstTrack
                }
                is LoadResult.SearchResult -> {
                    val firstTrack = loadResult.data.tracks.first()
                    val startedNow =  musicManager.playTrack(firstTrack)
                    if (startedNow) {
                        musicManager.replayTrack = firstTrack
                    }
                }
                is LoadResult.LoadFailed -> channel.createMessage("I couldn't find what I was searching for: \"$title\"")
                is LoadResult.NoMatches -> channel.createMessage("I couldn't find what I was searching for: \"$title\"")

            }
        }

    }
}
