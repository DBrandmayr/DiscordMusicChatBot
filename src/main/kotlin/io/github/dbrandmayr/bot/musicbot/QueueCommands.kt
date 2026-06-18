package io.github.dbrandmayr.bot.musicbot

import dev.arbjerg.lavalink.protocol.v4.LoadResult
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.event.message.MessageCreateEvent
import dev.schlaubi.lavakord.rest.loadItem
import io.github.dbrandmayr.bot.Config
import io.github.dbrandmayr.bot.Messages
import io.github.dbrandmayr.bot.fill

private const val CATEGORY = "🗂️ Queue Commands"
private val commandNames = Config.instance.commandNames

private fun getMusicManager(guildId: Snowflake) = LavalinkManager.getMusicManager(guildId)

object QueueCommand : Command {
    override val names = commandNames.queue
    override val category = CATEGORY
    override val description = "Shows the current queue"

    override suspend fun execute(args: List<String>, event: MessageCreateEvent) {
        val channel = event.message.channel
        val queue = getMusicManager(getGuildId(event)).getQueueSnapshot()
        if (queue.isEmpty()) {
            channel.createMessage(Messages.instance.common.queueEmpty)
            return
        }
        channel.createEmbed {
            title = Messages.instance.queue.list.embedTitle
            description = queue.mapIndexed { i, track -> "${i + 1}. **${track.info.title}**" }.joinToString("\n")
        }
    }
}

object ShuffleCommand : Command {
    override val names = commandNames.shuffle
    override val category = CATEGORY
    override val description = "Shuffles the queue"

    override suspend fun execute(args: List<String>, event: MessageCreateEvent) {
        val channel = event.message.channel
        val musicManager = getMusicManager(getGuildId(event))
        if (!musicManager.shuffleQueue()) {
            channel.createMessage(Messages.instance.common.queueEmpty)
            return
        }
        channel.createMessage(Messages.instance.queue.shuffle.shuffled)
    }
}

object InsertCommand : Command {
    override val names = commandNames.insert
    override val category = CATEGORY
    override val description = "Inserts a track at a position: insert <track> <position>"

    override suspend fun execute(args: List<String>, event: MessageCreateEvent) {
        val channel = event.message.channel
        val musicManager = getMusicManager(getGuildId(event))

        if (!isUserInSameChannel(event, musicManager.link)) {
            channel.createMessage(Messages.instance.common.notInSameChannel)
            return
        }
        val insertNumber = args.lastOrNull()?.toIntOrNull() ?: run {
            channel.createMessage(Messages.instance.common.invalidPosition)
            return
        }
        val query = args.dropLast(1).joinToString(" ")
        val search = if (query.startsWith("http", ignoreCase = true)) query else "ytsearch:$query"

        val (tracks, successMessage) = when (val loadResult = musicManager.link.loadItem(search)) {
            is LoadResult.TrackLoaded -> listOf(loadResult.data) to
                Messages.instance.queue.insert.inserted.fill(
                    "title" to loadResult.data.info.title,
                    "position" to insertNumber.toString()
                )
            is LoadResult.SearchResult -> {
                val firstTrack = loadResult.data.tracks.first()
                listOf(firstTrack) to Messages.instance.queue.insert.inserted.fill(
                    "title" to firstTrack.info.title,
                    "position" to insertNumber.toString()
                )
            }
            is LoadResult.PlaylistLoaded -> loadResult.data.tracks to
                Messages.instance.queue.insert.insertedPlaylist.fill(
                    "count" to loadResult.data.tracks.size.toString(),
                    "position" to insertNumber.toString()
                )
            is LoadResult.NoMatches -> {
                channel.createMessage(Messages.instance.common.searchNoResults.fill("query" to query))
                return
            }
            is LoadResult.LoadFailed -> {
                channel.createMessage(Messages.instance.common.searchFailed)
                return
            }
        }

        val message = when (musicManager.insertIntoQueue(insertNumber, tracks)) {
            GuildMusicManager.InsertResult.INSERTED -> successMessage
            GuildMusicManager.InsertResult.QUEUE_EMPTY -> Messages.instance.queue.insert.queueEmpty
            GuildMusicManager.InsertResult.OUT_OF_RANGE -> Messages.instance.common.positionOutOfRange
        }
        channel.createMessage(message)
    }
}

object RemoveCommand : Command {
    override val names = commandNames.remove
    override val category = CATEGORY
    override val description = "Removes a track from the queue"

    override suspend fun execute(args: List<String>, event: MessageCreateEvent) {
        val channel = event.message.channel
        val removeNumber = args.getOrNull(0)?.toIntOrNull() ?: run {
            channel.createMessage(Messages.instance.common.invalidPosition)
            return
        }
        if (removeNumber <= 0) {
            channel.createMessage(Messages.instance.queue.remove.positionMustBePositive)
            return
        }
        val removedTrack = getMusicManager(getGuildId(event)).removeFromQueue(removeNumber) ?: run {
            channel.createMessage(Messages.instance.common.positionOutOfRange)
            return
        }
        channel.createMessage(Messages.instance.queue.remove.removed.fill("title" to removedTrack.info.title))
    }
}

val queueCommands: List<Command> = listOf(QueueCommand, ShuffleCommand, InsertCommand, RemoveCommand)