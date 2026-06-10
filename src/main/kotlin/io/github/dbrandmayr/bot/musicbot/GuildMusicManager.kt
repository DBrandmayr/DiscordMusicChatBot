package io.github.dbrandmayr.bot.musicbot

import dev.arbjerg.lavalink.protocol.v4.Track
import dev.schlaubi.lavakord.audio.Link
import dev.schlaubi.lavakord.audio.TrackEndEvent
import dev.schlaubi.lavakord.audio.TrackStartEvent
import dev.schlaubi.lavakord.audio.on
import dev.schlaubi.lavakord.audio.player.Player
import dev.schlaubi.lavakord.audio.player.applyFilters
import dev.schlaubi.lavakord.rest.getPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock


class GuildMusicManager(val link: Link) {
    val player: Player = link.player
    private val queueMutex = Mutex()
    private val trackQueue: MutableList<Track> = mutableListOf()
    var replayTrack: Track? = null

    /** Outcome of an [insertIntoQueue] call, so callers can pick the right user-facing message. */
    enum class InsertResult { INSERTED, QUEUE_EMPTY, OUT_OF_RANGE }

    /**
     * The track Lavalink is actually playing right now, queried live from the node.
     *
     * This is the single source of truth: it is fetched fresh from Lavalink on every call and can
     * therefore never drift out of sync, unlike a value reconstructed from the (lossy, reorderable)
     * event stream. The track events below are used purely as triggers, never as state.
     */
    suspend fun currentTrack(): Track? = link.node.getPlayer(link.guildId).track
    suspend fun currentPosition(): Long?  = currentTrack()?.info?.position


    init {
        CoroutineScope(Dispatchers.Default).launch {
            setPlayerVolume(50)
        }

        player.on<TrackStartEvent> {
            println("Track started: ${track.info.title}")
            replayTrack = track
        }

        player.on<TrackEndEvent> {
            println("Track ended: ${track.info.title} - ${reason.name}")
            if (reason.mayStartNext) {
                queueMutex.withLock {
                    if (trackQueue.isNotEmpty()) {
                        player.playTrack(trackQueue.removeAt(0))
                    }
                }
            }
        }
    }

    /**
     * Plays the given track either immediately or adds it to the track queue if a track is already
     * playing or tracks are waiting to be played.
     *
     * @param track The track to be played or queued.
     * @return `true` if the track started playing immediately, `false` if the track was added to the queue.
     */
    suspend fun playTrack(track: Track): Boolean {
        return queueMutex.withLock {
            if (currentTrack() != null || trackQueue.isNotEmpty()) {
                trackQueue.add(track)
                false
            } else {
                player.playTrack(track)
                true
            }
        }
    }

    suspend fun stop() {
        queueMutex.withLock {
            trackQueue.clear()
        }
        player.stopTrack()
    }

    suspend fun skip() {
        queueMutex.withLock {
            if (trackQueue.isNotEmpty()) {
                player.playTrack(trackQueue.removeAt(0))
            } else {
                player.stopTrack()
            }
        }
    }

    suspend fun setPlayerVolume(desiredVolume: Int) {
        val clampedVolume = desiredVolume.coerceIn(0, 100)
        val volumeFloat = clampedVolume / 100f
        player.applyFilters {
            volume = volumeFloat
        }
    }

    suspend fun getQueueSnapshot(): List<Track> = queueMutex.withLock {
        trackQueue.toList()
    }

    /** Appends [tracks] to the end of the queue. */
    suspend fun addToQueue(tracks: List<Track>) = queueMutex.withLock {
        trackQueue.addAll(tracks)
        Unit
    }

    /** Prepends [tracks] so they play before everything currently queued. */
    suspend fun addToQueueFront(tracks: List<Track>) = queueMutex.withLock {
        trackQueue.addAll(0, tracks)
        Unit
    }

    /** Shuffles the queue. Returns `false` if the queue was empty. */
    suspend fun shuffleQueue(): Boolean = queueMutex.withLock {
        if (trackQueue.isEmpty()) return@withLock false
        trackQueue.shuffle()
        true
    }

    /**
     * Inserts [tracks] at the 1-based [position], with the bounds check performed atomically under
     * the lock so the position cannot become stale between validation and insertion.
     */
    suspend fun insertIntoQueue(position: Int, tracks: List<Track>): InsertResult = queueMutex.withLock {
        when {
            trackQueue.isEmpty() -> InsertResult.QUEUE_EMPTY
            position < 1 || position > trackQueue.size -> InsertResult.OUT_OF_RANGE
            else -> {
                trackQueue.addAll(position - 1, tracks)
                InsertResult.INSERTED
            }
        }
    }

    /** Removes and returns the track at the 1-based [position], or `null` if it is out of range. */
    suspend fun removeFromQueue(position: Int): Track? = queueMutex.withLock {
        if (position < 1 || position > trackQueue.size) null
        else trackQueue.removeAt(position - 1)
    }
}