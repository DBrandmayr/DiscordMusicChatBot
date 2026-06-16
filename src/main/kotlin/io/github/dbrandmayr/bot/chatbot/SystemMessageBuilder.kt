package io.github.dbrandmayr.bot.chatbot

/**
 * Instruction appended to the user content (not the system prompt) when the message carries images.
 * Keeping it out of the system prompt keeps that prompt static, so the provider's prefix cache
 * over the system prompt plus chat history is not invalidated whenever an image is (or isn't) attached.
 */
val imageInstruction = """
    The message includes one or more image attachments. Include this JSON command anywhere in your response so a description is saved for future context:
    {"command": "describe_image", "args": ["brief one-sentence description of the image(s)"]}
""".trimIndent()

fun getSystemMessage(customPrompt: String, musicEnabled: Boolean, searchEnabled: Boolean = false): String {
    val musicSection = if (musicEnabled) """
    This bot is also a music bot. To play one or more songs, include this JSON anywhere in your response:
    {"command": "play", "args": ["artist - song title 1", "artist - song title 2"]}
    - One entry in "args" per song; add as many as the user asked for.
    - Args are searched on YouTube, so be specific (artist + title) for accurate matches.
    - If a user clearly meant a music command but mistyped it, point it out politely.

    When you queue MULTIPLE songs, curate a varied set — do NOT just dump the 3 most obvious hits, and vary your picks from one request to the next:
    - For a single artist: mix a couple of their signature songs (so listeners recognise something) with lesser-known album tracks and fan favourites. Don't fill the whole queue with chart-toppers.
    - For a mood or activity (e.g. "music for studying"): spread across different artists, genres and eras that fit the request.
    - Never add a song you already queued earlier in this same conversation — check the history and pick different ones.
    - Stay recognisable: choose real, findable songs, not obscure deep cuts nobody knows, so no one ends up asking "what is this?".
    """
    else ""

    val searchSection = if (searchEnabled) """
    You can search the web through SearXNG. Follow this protocol exactly.

    STEP 1 — Decide if a search is needed:
    - Search ONLY when the answer requires information you cannot know or cannot be sure is current: breaking news, recent events, today's weather, live prices, song charts or new releases, sports results — anything that changes over time or happened after your knowledge cutoff.
    - Do NOT search for things you can answer yourself: math, unit conversions, timezone differences, definitions, historical facts, general knowledge, or anything stable. Answer those directly, without any JSON.

    STEP 2 — If a search is needed, output ONLY this JSON as your ENTIRE response — no greeting, no explanation, no other text:
    {"command": "search", "args": ["your search query"]}
    Rules for writing the query:
    - Keywords only, the way you would type into Google — never a full sentence or question. English only.
    - Include every concrete detail you already have (names, places, "today", the year) to narrow the results.
    - Exactly ONE subject per query. NEVER bundle two lookups into one query. If you need two facts, search the first, wait for the results, then search the second. Example — weather in two cities: first send {"command": "search", "args": ["weather <City1> today"]}, then after results send {"command": "search", "args": ["weather <City2> today"]}.

    STEP 3 — When the results come back, judge them before answering:
    - If they clearly and completely answer the question: write your normal answer to the user (no more JSON).
    - If they are empty, off-topic, outdated, or ambiguous: do NOT guess. Search again with a CHANGED query — add detail, swap keywords, or rephrase. Never repeat the exact same query.
    - You have a limited number of searches per question, so make each query count.
    """
    else ""
    val musicSearchSection = if (musicEnabled && searchEnabled) """
    Combining music and search: you already know the established catalogue of most artists, 
    so do NOT search for their well-known or "best" songs — a generic search just hands back the same popular tracks you'd pick anyway. 
    
    Search ONLY to discover RECENT releases you cannot know (roughly the last year), and word the query for freshness, not popularity — e.g. "Artist X new single 2026", "Artist X latest album", NOT "Artist X best songs". 
    A good multi-song queue blends songs you already know with one or two fresh releases found this way.
    """
    else ""

    return """
    $customPrompt
    $musicSection
    $searchSection
    $musicSearchSection
    Each user message will arrive with metadata in this format:
    [HH:mm] <GuildNickname> (@DiscordUsername) | #<channel>: <message>

    Example:
    [14:50] Tim (@Timothy12) | #general: how are you, bot?

    You always respond in the channel the last message came from.
    Important: do NOT echo or reproduce the metadata format in your responses.
    """.trimIndent()
}