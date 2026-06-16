package io.github.dbrandmayr.bot.chatbot.search

import io.github.dbrandmayr.bot.Config
import io.github.dbrandmayr.bot.chatClient
import io.github.dbrandmayr.bot.chatbot.ApiMessage
import io.github.dbrandmayr.bot.chatbot.BotCommandClass
import io.github.dbrandmayr.bot.searxngClient
import kotlinx.serialization.json.Json
import java.time.LocalDate

/**
 * System prompt for the isolated research agent. It shares NO persona with the chat bot — its only
 * job is to turn a plain-language information need into good web searches, judge the results, and
 * report back the facts. All the "how to search" knowledge lives here, so the bot prompt only has to
 * decide *whether* a search is needed.
 */
private val searchAgentPrompt = """
    You are a focused web-research assistant. You receive ONE research task, find accurate and
    up-to-date information for it, then report back. You have no personality — you only research and report.

    You search through SearXNG. To run a search, output ONLY this JSON as your ENTIRE response — nothing else:
    {"command": "search", "args": ["your search query"]}

    Writing good queries:
    - Keyword-style, the way you would type into Google — never a full sentence or question. English only.
    - Include every concrete detail from the task (names, places, the current year).
    - Exactly ONE subject per query. If the task needs several facts, look them up one at a time across multiple searches.
    - For music (newest/latest songs or releases): word the query for RECENCY, not popularity —
      e.g. "Artist X new single 2026", "Artist X latest album 2026", "Artist X discography", NOT "Artist X best songs".

    After each result set, judge it:
    - If it answers the task: stop searching and write your report (see below).
    - If it is empty, off-topic or unclear: search AGAIN with a CHANGED query — different keywords, more or
      less specific, an alternative spelling. Never repeat an identical query.
    - Do NOT conclude that something or someone does not exist after one weak search. A name you do not
      recognise is probably real but newer than your knowledge — try at least 2-3 different queries before
      reporting that nothing was found.

    When you have what you need, reply in PLAIN TEXT (no JSON): a concise, factual report of what is
    relevant to the task — concrete names, titles, dates, numbers. No filler, no opinions, no persona.
    If after several honest attempts you genuinely found nothing, say so plainly and briefly.
""".trimIndent()

/** Result of one research task: the factual summary, plus the actual queries the agent ran (for display). */
data class SearchAgentResult(val summary: String, val queries: List<String>)

/**
 * Runs the research agent for a single information need. The agent loops internally — search, judge,
 * re-search — up to [SearxngConfig.maxSearches] times, then returns a plain-text factual summary.
 */
suspend fun runSearchAgent(task: String): SearchAgentResult {
    val cfg = Config.instance.chatbot.searxng
    val model = cfg.model.ifBlank { Config.instance.chatbot.openai.model }
    val queries = mutableListOf<String>()

    val messages = mutableListOf(
        ApiMessage("system", searchAgentPrompt),
        ApiMessage("user", "Today's date is ${LocalDate.now()}.\nResearch task: $task")
    )

    var response = chatClient.sendMessage(messages, model = model, temperature = cfg.temperature)

    repeat(cfg.maxSearches) {
        val query = extractSearchArg(response) ?: return SearchAgentResult(response.trim(), queries)
        queries += query
        val results = try {
            searxngClient.search(query)
        } catch (e: Exception) {
            println("SearXNG search failed: ${e.message}")
            "Search failed: ${e.message}"
        }
        messages += ApiMessage("assistant", response)
        messages += ApiMessage("user", "Results for \"$query\":\n$results")
        response = chatClient.sendMessage(messages, model = model, temperature = cfg.temperature)
    }

    // Budget exhausted while the agent still wanted to search — force a plain-text summary.
    if (extractSearchArg(response) != null) {
        messages += ApiMessage("assistant", response)
        messages += ApiMessage("user", "Search budget reached. Report what you found so far in plain text, with no further search commands.")
        response = chatClient.sendMessage(messages, model = model, temperature = cfg.temperature)
    }

    return SearchAgentResult(response.trim(), queries)
}

/** Extracts the first arg of a `{"command":"search","args":[...]}` JSON anywhere in [response], or null. */
fun extractSearchArg(response: String): String? {
    val jsonRegex = """\{.*?}""".toRegex()
    return jsonRegex.findAll(response).firstNotNullOfOrNull { match ->
        try {
            val cmd = Json.decodeFromString<BotCommandClass>(match.value)
            if (cmd.command == "search") cmd.args.firstOrNull() else null
        } catch (_: Exception) { null }
    }
}