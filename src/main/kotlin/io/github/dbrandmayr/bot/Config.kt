package io.github.dbrandmayr.bot

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import tools.jackson.dataformat.yaml.YAMLMapper
import tools.jackson.module.kotlin.kotlinModule
import java.io.File

@JsonIgnoreProperties(ignoreUnknown = true)
data class Config(
    val bot: BotConfig = BotConfig(),
    val lavalink: LavalinkConfig = LavalinkConfig(),
    val openai: OpenaiConfig = OpenaiConfig(),
    val chatbot: ChatbotConfig = ChatbotConfig()
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
    val password: String = "youshallnotpass"
)

data class OpenaiConfig(
    val key: String = "",
    val model: String = "gpt-4.1"
)

data class ChatbotConfig(
    val enabled: Boolean = true,
    val systemPrompt: String = "You are a helpful and friendly Discord bot assistant. Answer questions, help with tasks, and keep the conversation fun and engaging."
)