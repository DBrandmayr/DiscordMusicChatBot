package io.github.dbrandmayr.bot.musicbot

import dev.kord.common.entity.Snowflake
import dev.schlaubi.lavakord.LavaKord
import dev.schlaubi.lavakord.kord.getLink
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.time.Duration.Companion.seconds

object LavalinkManager {
    private lateinit var lavalink: LavaKord
    private val guildMusicManagers = mutableMapOf<Snowflake, GuildMusicManager>()

    fun initialize(lavalink: LavaKord) {
        this.lavalink = lavalink
    }

    fun connect(host: String, port: Int, password: String) {
        CoroutineScope(Dispatchers.IO).launch {
            repeat(15) { attempt ->
                try {
                    Socket().use { it.connect(InetSocketAddress(host, port), 1000) }
                    lavalink.addNode("ws://$host:$port", password)
                    println("Connected to Lavalink.")
                    return@launch
                } catch (_: ConnectException) {
                    val waitSeconds = (attempt + 1) * 4
                    println("Lavalink not ready, retrying in ${waitSeconds}s... (${attempt + 1}/15)")
                    delay(waitSeconds.seconds)
                }
            }
            println("Could not connect to Lavalink after 15 attempts.")
        }
    }

    fun getMusicManager(guildId: Snowflake): GuildMusicManager {
        val link = lavalink.getLink(guildId)
        return guildMusicManagers.getOrPut(guildId) {
            GuildMusicManager(link)
        }
    }
}