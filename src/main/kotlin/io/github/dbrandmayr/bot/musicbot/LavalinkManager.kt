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


    /**
     * Establishes a connection to a Lavalink node using the specified host, port, and password.
     * If the connection fails, it will retry up to 15 times with increasing delays between attempts.
     *
     * @param host The hostname or IP address of the Lavalink node.
     * @param port The port on which the Lavalink node is listening.
     * @param password The password used for authentication with the Lavalink node.
     */
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