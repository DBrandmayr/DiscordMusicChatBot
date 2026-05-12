package io.github.dbrandmayr.bot.musicbot

import dev.arbjerg.lavalink.protocol.v4.LoadResult
import dev.arbjerg.lavalink.protocol.v4.Track
import dev.kord.core.event.message.MessageCreateEvent
import dev.schlaubi.lavakord.audio.Link
import dev.schlaubi.lavakord.rest.loadItem

private const val CATEGORY = "🎵 Music Commands"

object PlayCommand : Command {
    override val name = "play"
    override val aliases = listOf("p")
    override val category = CATEGORY
    override val description = "Plays a song or playlist"

    override suspend fun execute(args: List<String>, event: MessageCreateEvent) {
        val channel = event.message.channel
        val query = args.joinToString(" ")
        if (query.isBlank()) {
            channel.createMessage("Please provide a song name or URL to play.")
            return
        }
        val guildId = getGuildId(event)
        val voiceChannelId = event.member?.getVoiceStateOrNull()?.channelId ?: run {
            channel.createMessage("You need to be in a voice channel to play music.")
            return
        }

        val musicManager = LavalinkManager.getMusicManager(guildId)
        val link = musicManager.link
        if (link.state != Link.State.CONNECTED) link.connectAudio(voiceChannelId.value)

        val search = if (query.startsWith("http", ignoreCase = true)) query else "ytsearch:$query"
        when (val loadResult = link.loadItem(search)) {
            is LoadResult.TrackLoaded -> {
                val startedNow = musicManager.playTrack(loadResult.data)
                if (startedNow) {
                    channel.createMessage("Now playing: \"***${loadResult.data.info.title}***\"")
                    musicManager.replayTrack = loadResult.data
                } else {
                    channel.createMessage("Added \"***${loadResult.data.info.title}***\" to the queue.")
                }
            }
            is LoadResult.PlaylistLoaded -> {
                val firstTrack = loadResult.data.tracks.first()
                val remaining = loadResult.data.tracks.drop(1)
                musicManager.playTrack(firstTrack)
                musicManager.trackQueue.addAll(remaining)
                musicManager.replayTrack = firstTrack
                channel.createMessage("Now playing: \"***${firstTrack.info.title}***\" and added **${remaining.size}** more tracks to the queue.")
            }
            is LoadResult.SearchResult -> {
                val firstTrack = loadResult.data.tracks.first()
                val startedNow = musicManager.playTrack(firstTrack)
                if (startedNow) {
                    channel.createMessage("Now playing: \"***${firstTrack.info.title}***\"")
                    musicManager.replayTrack = firstTrack
                } else {
                    channel.createMessage("Added \"***${firstTrack.info.title}***\" to the queue.")
                }
            }
            is LoadResult.NoMatches -> channel.createMessage("No results found for \"$query\".")
            is LoadResult.LoadFailed -> channel.createMessage("Something went wrong while searching. Please try again.")
        }
    }
}

object PauseCommand : Command {
    override val name = "pause"
    override val category = CATEGORY
    override val description = "Pauses the current track"

    override suspend fun execute(args: List<String>, event: MessageCreateEvent) {
        getPlayer(event).pause(true)
        event.message.channel.createMessage("Paused.")
    }
}

object ResumeCommand : Command {
    override val name = "resume"
    override val category = CATEGORY
    override val description = "Resumes the current track"

    override suspend fun execute(args: List<String>, event: MessageCreateEvent) {
        getPlayer(event).pause(false)
        event.message.channel.createMessage("Resumed.")
    }
}

object StopCommand : Command {
    override val name = "stop"
    override val category = CATEGORY
    override val description = "Stops playback and clears the queue"

    override suspend fun execute(args: List<String>, event: MessageCreateEvent) {
        LavalinkManager.getMusicManager(getGuildId(event)).stop()
        event.message.channel.createMessage("Stopped and cleared the queue.")
    }
}

object LeaveCommand : Command {
    override val name = "leave"
    override val category = CATEGORY
    override val description = "Leaves the voice channel"

    override suspend fun execute(args: List<String>, event: MessageCreateEvent) {
        val musicManager = LavalinkManager.getMusicManager(getGuildId(event))
        musicManager.stop()
        musicManager.link.destroy()
        event.message.channel.createMessage("Goodbye!")
    }
}

object SkipCommand : Command {
    override val name = "skip"
    override val aliases = listOf("s")
    override val category = CATEGORY
    override val description = "Skips the current track"

    override suspend fun execute(args: List<String>, event: MessageCreateEvent) {
        val channel = event.message.channel
        val musicManager = LavalinkManager.getMusicManager(getGuildId(event))
        val track = musicManager.currentTrack
        if (track != null) {
            channel.createMessage("Skipped: \"***${track.info.title}***\"")
            musicManager.skip()
        } else {
            channel.createMessage("Nothing is currently playing.")
        }
    }
}

object PlayingCommand : Command {
    override val name = "playing"
    override val aliases = listOf("np")
    override val category = CATEGORY
    override val description = "Shows what's currently playing"

    override suspend fun execute(args: List<String>, event: MessageCreateEvent) {
        val channel = event.message.channel
        val musicManager = LavalinkManager.getMusicManager(getGuildId(event))
        val playingTrack = musicManager.currentTrack ?: run {
            channel.createMessage("Nothing is currently playing.")
            return
        }
        val livePosition = musicManager.link.player.position
        val fmt = { ms: Long -> "%02d:%02d".format(ms / 1000 / 60, ms / 1000 % 60) }
        channel.createMessage("Now playing: \"**${playingTrack.info.title}**\"\n*${fmt(livePosition)} / ${fmt(playingTrack.info.length)}*")
    }
}

object ReplayCommand : Command {
    override val name = "replay"
    override val aliases = listOf("r", "repeat")
    override val category = CATEGORY
    override val description = "Replays the last track (optional: number of times)"

    override suspend fun execute(args: List<String>, event: MessageCreateEvent) {
        val channel = event.message.channel
        val musicManager = LavalinkManager.getMusicManager(getGuildId(event))
        val replayTrack: Track = musicManager.replayTrack ?: run {
            channel.createMessage("No track has been played yet.")
            return
        }
        var replayAmount = 1
        if (args.isNotEmpty()) {
            try {
                replayAmount = args[0].toInt()
                if (replayAmount == 0 || replayAmount >= 100) {
                    channel.createMessage("That's out of range. Using 1 instead.")
                    replayAmount = 1
                }
            } catch (_: NumberFormatException) {
                channel.createMessage("That's not a valid number, ignoring it.")
            }
        }
        val replayList = List(replayAmount) { replayTrack }
        if (musicManager.currentTrack != null) {
            musicManager.trackQueue.addAll(0, replayList)
        } else {
            musicManager.playTrack(replayList[0])
            musicManager.trackQueue.addAll(0, replayList.drop(1))
        }
        channel.createMessage("\"***${replayTrack.info.title}***\" will play next.")
    }
}

object SeekCommand : Command {
    override val name = "seek"
    override val aliases = listOf("time")
    override val category = CATEGORY
    override val description = "Seeks to a position in the track (mm:ss or seconds)"

    override suspend fun execute(args: List<String>, event: MessageCreateEvent) {
        val channel = event.message.channel
        val musicManager = LavalinkManager.getMusicManager(getGuildId(event))

        if (!isUserInSameChannel(event, musicManager.link)) {
            channel.createMessage("You need to be in the same voice channel as the bot.")
            return
        }
        if (args.isEmpty()) {
            channel.createMessage("Please provide a time to seek to.")
            return
        }
        if (musicManager.currentTrack == null) {
            channel.createMessage("Nothing is currently playing.")
            return
        }
        val timeMillis = parseTimeToMillis(args[0]) ?: run {
            channel.createMessage("Invalid time format. Use mm:ss or seconds.")
            return
        }
        if (timeMillis < 0 || timeMillis > (musicManager.currentTrack?.info?.length ?: 0)) {
            channel.createMessage("Time is out of range for the current track.")
            return
        }
        musicManager.link.player.seekTo(timeMillis)
        channel.createMessage("Seeked to **${"%02d:%02d".format(timeMillis / 1000 / 60, timeMillis / 1000 % 60)}**.")
    }

    private fun parseTimeToMillis(time: String): Long? = try {
        if (time.contains(":")) {
            val parts = time.split(":").map { it.toInt() }
            if (parts.size == 2) (parts[0] * 60 + parts[1]) * 1000L else null
        } else {
            time.toInt() * 1000L
        }
    } catch (_: NumberFormatException) { null }
}

object VolumeCommand : Command {
    override val name = "volume"
    override val aliases = listOf("v")
    override val category = CATEGORY
    override val description = "Sets the volume (0–200)"

    override suspend fun execute(args: List<String>, event: MessageCreateEvent) {
        val channel = event.message.channel
        val musicManager = LavalinkManager.getMusicManager(getGuildId(event))

        if (!isUserInSameChannel(event, musicManager.link)) {
            channel.createMessage("You need to be in the same voice channel as the bot.")
            return
        }
        if (args.isEmpty()) {
            val current = musicManager.player.filters.volume?.times(100)?.toInt()
            channel.createMessage("Current volume: **$current**.")
            return
        }
        val volume = args[0].toIntOrNull() ?: run {
            channel.createMessage("Invalid volume. Please provide a number between 0 and 200.")
            return
        }
        if (volume < 0 || volume > 200) {
            channel.createMessage("Volume must be between 0 and 200.")
            return
        }
        musicManager.setPlayerVolume(volume)
        channel.createMessage("Volume set to **$volume**.")
    }
}

val musicCommands: List<Command> = listOf(
    PlayCommand, PauseCommand, ResumeCommand, StopCommand, LeaveCommand,
    SkipCommand, PlayingCommand, ReplayCommand, SeekCommand, VolumeCommand
)