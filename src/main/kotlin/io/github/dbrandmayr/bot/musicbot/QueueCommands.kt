package io.github.dbrandmayr.bot.musicbot

import dev.arbjerg.lavalink.protocol.v4.LoadResult
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.event.message.MessageCreateEvent
import dev.schlaubi.lavakord.rest.loadItem

private const val CATEGORY = "🗂️ Queue Commands"

object QueueCommand : Command {
    override val name = "queue"
    override val aliases = listOf("q")
    override val category = CATEGORY
    override val description = "Shows the current queue"

    override suspend fun execute(args: List<String>, event: MessageCreateEvent) {
        val channel = event.message.channel
        val queue = LavalinkManager.getMusicManager(getGuildId(event)).getQueueSnapshot()
        if (queue.isEmpty()) {
            channel.createMessage("The queue is currently empty.")
            return
        }
        channel.createEmbed {
            title = "Queue"
            description = queue.mapIndexed { i, track -> "${i + 1}. **${track.info.title}**" }.joinToString("\n")
        }
    }
}

object ShuffleCommand : Command {
    override val name = "shuffle"
    override val aliases = listOf("mix")
    override val category = CATEGORY
    override val description = "Shuffles the queue"

    override suspend fun execute(args: List<String>, event: MessageCreateEvent) {
        val channel = event.message.channel
        val musicManager = LavalinkManager.getMusicManager(getGuildId(event))
        if (musicManager.trackQueue.isEmpty()) {
            channel.createMessage("The queue is currently empty.")
            return
        }
        musicManager.trackQueue.shuffle()
        channel.createMessage("Queue shuffled.")
    }
}

object InsertCommand : Command {
    override val name = "insert"
    override val aliases = listOf("put")
    override val category = CATEGORY
    override val description = "Inserts a track at a position: insert <track> <position>"

    override suspend fun execute(args: List<String>, event: MessageCreateEvent) {
        val channel = event.message.channel
        val musicManager = LavalinkManager.getMusicManager(getGuildId(event))

        if (!isUserInSameChannel(event, musicManager.link)) {
            channel.createMessage("You need to be in the same voice channel as the bot.")
            return
        }
        if (musicManager.trackQueue.isEmpty()) {
            channel.createMessage("The queue is empty. Nothing to insert into.")
            return
        }
        val insertNumber = args.lastOrNull()?.toIntOrNull() ?: run {
            channel.createMessage("Please provide a valid position number.")
            return
        }
        if (insertNumber >= musicManager.trackQueue.size + 1) {
            channel.createMessage("Position is out of range for the current queue.")
            return
        }
        val query = args.dropLast(1).joinToString(" ")
        val search = if (query.startsWith("http", ignoreCase = true)) query else "ytsearch:$query"

        when (val loadResult = musicManager.link.loadItem(search)) {
            is LoadResult.TrackLoaded -> {
                musicManager.trackQueue.add(insertNumber - 1, loadResult.data)
                channel.createMessage("Inserted **\"${loadResult.data.info.title}\"** at position **${insertNumber}** in the queue.")
            }
            is LoadResult.PlaylistLoaded -> {
                musicManager.trackQueue.addAll(insertNumber - 1, loadResult.data.tracks)
                channel.createMessage("Inserted **${loadResult.data.tracks.size}** tracks at position **${insertNumber}** in the queue.")
            }
            is LoadResult.SearchResult -> {
                val firstTrack = loadResult.data.tracks.first()
                musicManager.trackQueue.add(insertNumber - 1, firstTrack)
                channel.createMessage("Inserted **\"${firstTrack.info.title}\"** at position **${insertNumber}** in the queue.")
            }
            is LoadResult.NoMatches -> channel.createMessage("No results found for \"$query\".")
            is LoadResult.LoadFailed -> channel.createMessage("Something went wrong while searching. Please try again.")
        }
    }
}

object RemoveCommand : Command {
    override val name = "remove"
    override val category = CATEGORY
    override val description = "Removes a track from the queue"

    override suspend fun execute(args: List<String>, event: MessageCreateEvent) {
        val channel = event.message.channel
        val removeNumber = args.getOrNull(0)?.toIntOrNull() ?: run {
            channel.createMessage("Please provide a valid position number.")
            return
        }
        val trackQueue = LavalinkManager.getMusicManager(getGuildId(event)).trackQueue
        if (removeNumber <= 0) {
            channel.createMessage("Position must be greater than 0.")
            return
        }
        if (removeNumber > trackQueue.size) {
            channel.createMessage("Position is out of range for the current queue.")
            return
        }
        try {
            val removed = trackQueue.removeAt(removeNumber - 1)
            channel.createMessage("Removed \"***${removed.info.title}***\" from the queue.")
        } catch (_: IndexOutOfBoundsException) {
            channel.createMessage("Something went wrong!")
        }
    }
}

val queueCommands: List<Command> = listOf(QueueCommand, ShuffleCommand, InsertCommand, RemoveCommand)