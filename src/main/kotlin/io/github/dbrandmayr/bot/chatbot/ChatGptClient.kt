package io.github.dbrandmayr.bot.chatbot

import io.github.dbrandmayr.bot.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import tools.jackson.databind.DeserializationFeature
import tools.jackson.module.kotlin.jsonMapper
import tools.jackson.module.kotlin.kotlinModule
import tools.jackson.module.kotlin.readValue

data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatBotMessage>,
    val temperature: Double = 0.7
)

data class ChatBotMessage(
    val role: String, // "user", "assistant", or "system"
    val content: String
)

data class ChatCompletionResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: ChatBotMessage
)

class ChatGptClient(private val apiKey: String) {
    private val client = OkHttpClient()
    private val objectMapper = jsonMapper {
        addModule(kotlinModule())
        disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    }

    suspend fun sendMessage(messages: List<ChatBotMessage>): String {
        val url = "https://api.openai.com/v1/chat/completions"
        val requestBody = objectMapper.writeValueAsString(
            ChatCompletionRequest(
                model = Config.instance.openai.model,
                messages = messages
            )
        )

        val request = Request.Builder()
            .url(url)
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .addHeader("Authorization", "Bearer $apiKey")
            .build()

        return withContext(Dispatchers.IO){
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body.string()
                    throw Exception("Failed to fetch from OpenAI: ${response.message}, Body: $errorBody")
                }
                val responseBody = response.body.string()
                val chatResponse: ChatCompletionResponse = objectMapper.readValue(responseBody)
                chatResponse.choices.first().message.content
            }
        }
    }
}