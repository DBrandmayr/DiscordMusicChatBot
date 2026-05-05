package io.github.dbrandmayr.bot.musicbot

import dev.arbjerg.lavalink.protocol.v4.Track
import dev.schlaubi.lavakord.audio.Link
import dev.schlaubi.lavakord.audio.TrackEndEvent
import dev.schlaubi.lavakord.audio.TrackStartEvent
import dev.schlaubi.lavakord.audio.on
import dev.schlaubi.lavakord.audio.player.Player
import dev.schlaubi.lavakord.audio.player.applyFilters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock


class GuildMusicManager(val link: Link) {
    val player: Player = link.player
    private val queueMutex = Mutex()
    val trackQueue: MutableList<Track> = mutableListOf()
    var currentTrack: Track? = null
    var replayTrack: Track? = null

    init {
        CoroutineScope(Dispatchers.Default).launch {
            setPlayerVolume(50)
        }

        player.on<TrackStartEvent> {
            println("Track started: ${track.info.title}")
            currentTrack = track
            replayTrack = track
        }

        player.on<TrackEndEvent> {
            println("Track ended: ${track.info.title} - ${reason.name}")
            if (reason.mayStartNext){
                queueMutex.withLock {
                    if (trackQueue.isNotEmpty()) {
                        val nextTrack = trackQueue.removeAt(0)
                        currentTrack = nextTrack
                        player.playTrack(nextTrack)
                    } else{
                        currentTrack = null
                    }
                }
            }
        }
    }

    /**
     * Plays the given track either immediately or adds it to the track queue if a track is already playing.
     *
     * @param track The track to be played or queued.
     * @return `true` if the track started playing immediately, `false` if the track was added to the queue.
     */
    suspend fun playTrack(track: Track): Boolean{
        return queueMutex.withLock {
            if (currentTrack != null) {
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
            currentTrack = null
        }
        player.stopTrack()
    }

    suspend fun skip() {
        queueMutex.withLock {
            if (trackQueue.isNotEmpty()) {
                val nextTrack = trackQueue.removeAt(0)
                currentTrack = nextTrack
                player.playTrack(nextTrack)
            } else {
                currentTrack = null
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
}