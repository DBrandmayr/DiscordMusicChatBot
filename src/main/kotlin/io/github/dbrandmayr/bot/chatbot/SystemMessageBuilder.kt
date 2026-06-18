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
    This bot is also a music bot. When a user asks you to play, queue or add songs, just DO it — include the play command in your response, don't merely talk about it or ask for confirmation first. To play one or more songs, include this JSON anywhere in your response:
    {"command": "play", "args": ["artist - song title 1", "artist - song title 2"]}
    - One entry in "args" per song. Add exactly what the user wants: if they named specific songs, include ALL of them; if they gave a number, match it; if they didn't, pick a sensible amount for the request (a few for "some songs", more for "a playlist") — do NOT always default to three.
    - Each entry must be a REAL, specific song (artist + exact title), because it is searched on YouTube. ${if (searchEnabled) "NEVER put a placeholder like \"latest song\", \"the new one\" or \"the last song\" as a title — if you don't know the exact title, look it up first (see below), then play the real one." else ""}
    - If a user clearly meant a music command but mistyped it, point it out politely.

    When you queue MULTIPLE songs, curate a varied set — do not just dump the most obvious hits, and vary your picks from one request to the next:
    - For a single artist: mix a couple of their signature songs (so listeners recognise something) with lesser-known album tracks and fan favourites. Don't fill the whole queue with chart-toppers.
    - For a mood or activity (e.g. "music for studying"): spread across different artists, genres and eras that fit the request.
    - Never add a song you already queued earlier in this same conversation — check the history and pick different ones.
    - Stay recognisable: choose real, findable songs, not obscure deep cuts nobody knows, so no one ends up asking "what is this?".
    """
    else ""

    val searchSection = if (searchEnabled) """
    You can look things up on the web, but you do NOT search yourself — a separate research assistant does that and hands you the findings. Your only job is to decide WHEN something needs looking up.
    Delegate a lookup when the answer needs information you cannot be sure of from your own knowledge: recent or real-time facts (news, today's weather, live prices, sports results), or any specific name, song, release, person or fact you do not clearly know. To delegate, output ONLY this JSON as your ENTIRE response — no other text:
    {"command": "search", "args": ["plain-language description of what to find out"]}
    Just describe what you need to know in plain words — do NOT write keyword queries; the assistant handles that. You will then receive the findings and answer normally.
    Do NOT delegate things you can answer yourself: math, unit/timezone conversions, definitions, historical facts, general knowledge — answer those directly.
    If a request needs BOTH a lookup and an action (e.g. playing a song whose exact title you are not sure of), delegate the lookup FIRST — a search must be your entire response, so you cannot search and act in the same message. You will get the findings back and carry out the action then. Never guess or invent a detail just to avoid searching.
    Never claim that something or someone does not exist just because you do not recognise the name. If a user talks about a band, song, person or thing as if it is real, assume it may be newer than your knowledge and delegate a lookup to check before doubting them.
    """
    else ""
    val musicSearchSection = if (musicEnabled && searchEnabled) """
    When a user asks for an artist's latest, newest or recent songs or releases, always delegate a lookup first — you cannot know releases after your knowledge cutoff. Example: {"command": "search", "args": ["latest songs and releases by <artist>, with release dates"]}.
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