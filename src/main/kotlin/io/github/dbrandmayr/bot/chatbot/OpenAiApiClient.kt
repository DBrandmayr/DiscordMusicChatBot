package io.github.dbrandmayr.bot.chatbot

import com.fasterxml.jackson.annotation.JsonProperty
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
import java.util.concurrent.TimeUnit

// Used for history storage and DB persistence, content is always plain text
data class ChatBotMessage(
    val role: String,
    val content: String
)

// Used only for API calls, content can be a String or a list of content parts (text + images)
data class ApiMessage(
    val role: String,
    val content: Any
)

data class TextPart(val type: String = "text", val text: String)

data class ImageUrlPart(
    val type: String = "image_url",
    @field:JsonProperty("image_url") val imageUrl: Map<String, String>
)

data class ChatCompletionRequest(
    val model: String,
    val messages: List<ApiMessage>,
    val temperature: Double = Config.instance.chatbot.temperature
)

data class ChatCompletionResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: ChatBotMessage
)

class ChatGptClient(private val apiKey: String) {
    private val client = OkHttpClient.Builder()
        .readTimeout(Config.instance.chatbot.openai.timeoutSeconds, TimeUnit.SECONDS)
        .build()
    private val objectMapper = jsonMapper {
        addModule(kotlinModule())
        disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    }

    suspend fun sendMessage(
        messages: List<ApiMessage>,
        model: String = Config.instance.chatbot.openai.model,
        temperature: Double = Config.instance.chatbot.temperature
    ): String {
        val url = Config.instance.chatbot.openai.completionsUrl
        val requestBody = objectMapper.writeValueAsString(
            ChatCompletionRequest(
                model = model,
                messages = messages,
                temperature = temperature
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