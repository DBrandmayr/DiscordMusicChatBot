package io.github.dbrandmayr.bot.musicbot

import dev.arbjerg.lavalink.protocol.v4.LoadResult
import dev.arbjerg.lavalink.protocol.v4.Track
import dev.kord.common.entity.Snowflake
import dev.kord.core.event.message.MessageCreateEvent
import dev.schlaubi.lavakord.audio.Link
import dev.schlaubi.lavakord.rest.loadItem
import io.github.dbrandmayr.bot.Config
import io.github.dbrandmayr.bot.Messages
import io.github.dbrandmayr.bot.fill
import io.github.dbrandmayr.bot.formatDuration

private const val CATEGORY = "🎵 Music Commands"
private val commandNames = Config.instance.commandNames

private fun getMusicManager(guildId: Snowflake) = LavalinkManager.getMusicManager(guildId)

object PlayCommand : Command {
    override val names = commandNames.play
    override val category = CATEGORY
    override val description = "Plays a song or playlist"

    override suspend fun execute(args: List<String>, event: MessageCreateEvent) {
        val channel = event.message.channel
        val query = args.joinToString(" ")
        if (query.isBlank()) {
            channel.createMessage(Messages.instance.music.play.noQuery)
            return
        }
        val guildId = getGuildId(event)
        val voiceChannelId = event.member?.getVoiceStateOrNull()?.channelId ?: run {
            channel.createMessage(Messages.instance.music.play.notInVoiceChannel)
            return
        }

        val musicManager = getMusicManager(guildId)
        val link = musicManager.link
        if (link.state != Link.State.CONNECTED) link.connectAudio(voiceChannelId.value)

        val search = if (query.startsWith("http", ignoreCase = true)) query else "ytsearch:$query"
        when (val loadResult = link.loadItem(search)) {
            is LoadResult.TrackLoaded -> {
                val startedNow = musicManager.playTrack(loadResult.data)
                if (startedNow) {
                    channel.createMessage(Messages.instance.music.play.nowPlaying.fill("title" to loadResult.data.info.title))
                    musicManager.replayTrack = loadResult.data
                } else {
                    channel.createMessage(Messages.instance.music.play.addedToQueue.fill("title" to loadResult.data.info.title))
                }
            }
            is LoadResult.PlaylistLoaded -> {
                val firstTrack = loadResult.data.tracks.first()
                val remaining = loadResult.data.tracks.drop(1)
                musicManager.playTrack(firstTrack)
                musicManager.trackQueue.addAll(remaining)
                musicManager.replayTrack = firstTrack
                channel.createMessage(Messages.instance.music.play.nowPlayingPlaylist.fill(
                    "title" to firstTrack.info.title,
                    "count" to remaining.size.toString()
                ))
            }
            is LoadResult.SearchResult -> {
                val firstTrack = loadResult.data.tracks.first()
                val startedNow = musicManager.playTrack(firstTrack)
                if (startedNow) {
                    channel.createMessage(Messages.instance.music.play.nowPlaying.fill("title" to firstTrack.info.title))
                    musicManager.replayTrack = firstTrack
                } else {
                    channel.createMessage(Messages.instance.music.play.addedToQueue.fill("title" to firstTrack.info.title))
                }
            }
            is LoadResult.NoMatches -> channel.createMessage(Messages.instance.common.searchNoResults.fill("query" to query))
            is LoadResult.LoadFailed -> channel.createMessage(Messages.instance.common.searchFailed)
        }
    }
}

object PauseCommand : Command {
    override val names = commandNames.pause
    override val category = CATEGORY
    override val description = "Pauses the current track"

    override suspend fun execute(args: List<String>, event: MessageCreateEvent) {
        getPlayer(event).pause(true)
        event.message.channel.createMessage(Messages.instance.music.pause.paused)
    }
}

object ResumeCommand : Command {
    override val names = commandNames.resume
    override val category = CATEGORY
    override val description = "Resumes the current track"

    override suspend fun execute(args: List<String>, event: MessageCreateEvent) {
        getPlayer(event).pause(false)
        event.message.channel.createMessage(Messages.instance.music.resume.resumed)
    }
}

object StopCommand : Command {
    override val names = commandNames.stop
    override val category = CATEGORY
    override val description = "Stops playback and clears the queue"

    override suspend fun execute(args: List<String>, event: MessageCreateEvent) {
        getMusicManager(getGuildId(event)).stop()
        event.message.channel.createMessage(Messages.instance.music.stop.stopped)
    }
}

object LeaveCommand : Command {
    override val names = commandNames.leave
    override val category = CATEGORY
    override val description = "Leaves the voice channel"

    override suspend fun execute(args: List<String>, event: MessageCreateEvent) {
        val musicManager = getMusicManager(getGuildId(event))
        musicManager.stop()
        musicManager.link.destroy()
        event.message.channel.createMessage(Messages.instance.music.leave.goodbye)
    }
}

object SkipCommand : Command {
    override val names = commandNames.skip
    override val category = CATEGORY
    override val description = "Skips the current track"

    override suspend fun execute(args: List<String>, event: MessageCreateEvent) {
        val channel = event.message.channel
        val musicManager = getMusicManager(getGuildId(event))
        val track = musicManager.currentTrack
        if (track != null) {
            channel.createMessage(Messages.instance.music.skip.skipped.fill("title" to track.info.title))
            musicManager.skip()
        } else {
            channel.createMessage(Messages.instance.common.nothingPlaying)
        }
    }
}

object PlayingCommand : Command {
    override val names = commandNames.playing
    override val category = CATEGORY
    override val description = "Shows what's currently playing"

    override suspend fun execute(args: List<String>, event: MessageCreateEvent) {
        val channel = event.message.channel
        val musicManager = getMusicManager(getGuildId(event))
        val playingTrack = musicManager.currentTrack ?: run {
            channel.createMessage(Messages.instance.common.nothingPlaying)
            return
        }
        val livePosition = musicManager.link.player.position
        channel.createMessage(Messages.instance.music.playing.nowPlaying.fill(
            "title" to playingTrack.info.title,
            "position" to formatDuration(livePosition),
            "duration" to formatDuration(playingTrack.info.length)
        ))
    }
}

object ReplayCommand : Command {
    override val names = commandNames.replay
    override val category = CATEGORY
    override val description = "Replays the last track (optional: number of times)"

    override suspend fun execute(args: List<String>, event: MessageCreateEvent) {
        val channel = event.message.channel
        val musicManager = getMusicManager(getGuildId(event))
        val replayTrack: Track = musicManager.replayTrack ?: run {
            channel.createMessage(Messages.instance.music.replay.noTrackPlayed)
            return
        }
        var replayAmount = 1
        if (args.isNotEmpty()) {
            try {
                replayAmount = args[0].toInt()
                if (replayAmount == 0 || replayAmount >= 100) {
                    channel.createMessage(Messages.instance.music.replay.outOfRange)
                    replayAmount = 1
                }
            } catch (_: NumberFormatException) {
                channel.createMessage(Messages.instance.music.replay.invalidNumber)
            }
        }
        val replayList = List(replayAmount) { replayTrack }
        if (musicManager.currentTrack != null) {
            musicManager.trackQueue.addAll(0, replayList)
        } else {
            musicManager.playTrack(replayList[0])
            musicManager.trackQueue.addAll(0, replayList.drop(1))
        }
        channel.createMessage(Messages.instance.music.replay.willPlayNext.fill("title" to replayTrack.info.title))
    }
}

object SeekCommand : Command {
    override val names = commandNames.seek
    override val category = CATEGORY
    override val description = "Seeks to a position in the track (mm:ss or seconds)"

    override suspend fun execute(args: List<String>, event: MessageCreateEvent) {
        val channel = event.message.channel
        val musicManager = getMusicManager(getGuildId(event))

        if (!isUserInSameChannel(event, musicManager.link)) {
            channel.createMessage(Messages.instance.common.notInSameChannel)
            return
        }
        if (args.isEmpty()) {
            channel.createMessage(Messages.instance.music.seek.noTimeProvided)
            return
        }
        if (musicManager.currentTrack == null) {
            channel.createMessage(Messages.instance.common.nothingPlaying)
            return
        }
        val timeMillis = parseTimeToMillis(args[0]) ?: run {
            channel.createMessage(Messages.instance.music.seek.invalidTimeFormat)
            return
        }
        if (timeMillis < 0 || timeMillis > (musicManager.currentTrack?.info?.length ?: 0)) {
            channel.createMessage(Messages.instance.music.seek.timeOutOfRange)
            return
        }
        musicManager.link.player.seekTo(timeMillis)
        channel.createMessage(Messages.instance.music.seek.seeked.fill("time" to formatDuration(timeMillis)))
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
    override val names = commandNames.volume
    override val category = CATEGORY
    override val description = "Sets the volume (0–200)"

    override suspend fun execute(args: List<String>, event: MessageCreateEvent) {
        val channel = event.message.channel
        val musicManager = getMusicManager(getGuildId(event))

        if (!isUserInSameChannel(event, musicManager.link)) {
            channel.createMessage(Messages.instance.common.notInSameChannel)
            return
        }
        if (args.isEmpty()) {
            val current = musicManager.player.filters.volume?.times(100)?.toInt()
            channel.createMessage(Messages.instance.music.volume.currentVolume.fill("volume" to current.toString()))
            return
        }
        val volume = args[0].toIntOrNull() ?: run {
            channel.createMessage(Messages.instance.music.volume.invalidVolume)
            return
        }
        if (volume < 0 || volume > 200) {
            channel.createMessage(Messages.instance.music.volume.volumeOutOfRange)
            return
        }
        musicManager.setPlayerVolume(volume)
        channel.createMessage(Messages.instance.music.volume.volumeSet.fill("volume" to volume.toString()))
    }
}

val musicCommands: List<Command> = listOf(
    PlayCommand, PauseCommand, ResumeCommand, StopCommand, LeaveCommand,
    SkipCommand, PlayingCommand, ReplayCommand, SeekCommand, VolumeCommand
)