package io.github.dbrandmayr.bot

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import tools.jackson.dataformat.yaml.YAMLMapper
import tools.jackson.module.kotlin.kotlinModule
import java.io.File

@JsonIgnoreProperties(ignoreUnknown = true)
data class Config(
    val bot: BotConfig = BotConfig(),
    val chatbot: ChatbotConfig = ChatbotConfig(),
    val music: MusicConfig = MusicConfig(),
    @field:JsonProperty("command-names") val commandNames: CommandNames = CommandNames()
) {
    companion object {
        lateinit var instance: Config private set

        fun load(filePath: String): Config {
            val mapper = YAMLMapper.builder().addModule(kotlinModule()).build()
            instance = mapper.readValue(File(filePath), Config::class.java)
            println(if (instance.bot.token.isBlank()) "Bot token not set!" else "Bot token set!")
            println("Config loaded.")
            return instance
        }
    }
}

data class BotConfig(
    val token: String = "",
    val prefixes: Set<String> = setOf("!!"),
    val maxChatHistoryLength: Int = 50
)

data class LavalinkConfig(
    val host: String = "localhost",
    val port: Int = 2333,
    val password: String = "youshallnotpass",
    val secure: Boolean = false
)

data class OpenaiConfig(
    val key: String = "",
    val model: String = "",
    var completionsUrl: String = "https://api.openai.com/v1/chat/completions"
)

data class ChatbotConfig(
    val enabled: Boolean = true,
    val systemPrompt: String = "You are a helpful and friendly Discord bot assistant. Answer questions, help with tasks, and keep the conversation fun and engaging.",
    val openai: OpenaiConfig = OpenaiConfig()
)

data class MusicConfig(
    val enabled: Boolean = true,
    val lavalink: LavalinkConfig = LavalinkConfig()
)

data class CommandNames(
    val random: List<String> = listOf("random"),
    val coin: List<String> = listOf("coin", "c"),
    val wheel: List<String> = listOf("wheel", "w"),

    val play: List<String> = listOf("play", "p"),
    val pause: List<String> = listOf("pause"),
    val resume: List<String> = listOf("resume"),
    val stop: List<String> = listOf("stop"),
    val leave: List<String> = listOf("leave"),
    val skip: List<String> = listOf("skip", "s"),
    val playing: List<String> = listOf("playing"),
    val replay: List<String> = listOf("replay"),
    val seek: List<String> = listOf("seek"),
    val volume: List<String> = listOf("volume", "v"),

    val queue: List<String> = listOf("queue", "q"),
    val shuffle: List<String> = listOf("shuffle"),
    val insert: List<String> = listOf("insert", "put"),
    val remove: List<String> = listOf("remove"),

    val help: List<String> = listOf("help", "commands")
)