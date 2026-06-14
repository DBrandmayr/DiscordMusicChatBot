package io.github.dbrandmayr.bot.chatbot

import io.github.dbrandmayr.bot.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import tools.jackson.module.kotlin.jsonMapper
import tools.jackson.module.kotlin.kotlinModule
import tools.jackson.module.kotlin.readValue
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

@JsonIgnoreProperties(ignoreUnknown = true)
data class SearxngResult(
    val title: String = "",
    val url: String = "",
    val content: String = ""
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SearxngResponse(val results: List<SearxngResult> = emptyList())

class SearxngClient {
    private val client = OkHttpClient.Builder()
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    private val objectMapper = jsonMapper { addModule(kotlinModule()) }

    suspend fun search(query: String): String {
        val config = Config.instance.chatbot.searxng
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "${config.url}/search?q=$encodedQuery&format=json"

        return withContext(Dispatchers.IO) {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("SearXNG error: ${response.message}")
                val searxngResponse: SearxngResponse = objectMapper.readValue(response.body.string())
                formatResults(searxngResponse.results.take(config.maxResults))
            }
        }
    }

    private fun formatResults(results: List<SearxngResult>): String {
        if (results.isEmpty()) return "No results found."
        return results.mapIndexed { i, r ->
            "${i + 1}. ${r.title}\n${r.url}\n${r.content}"
        }.joinToString("\n\n")
    }
}