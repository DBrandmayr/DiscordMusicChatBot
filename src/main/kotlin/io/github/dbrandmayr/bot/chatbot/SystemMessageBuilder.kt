package io.github.dbrandmayr.bot.chatbot

fun getSystemMessage(customPrompt: String, musicEnabled: Boolean, hasImages: Boolean = false): String {
    val musicSection = if (musicEnabled) """
    Because this bot is also a music bot, there might be instances where a user intended to write
    a music command but mistyped it; in such cases, let them know politely.

    You are able to trigger music playback by including a JSON command anywhere in your response.
    If someone asks you to play one or more songs, include exactly this JSON (no extra whitespace needed):
    {"command": "play", "args": ["song title 1", "song title 2"]}
    Add as many or as few entries to "args" as needed. The play command automatically queues tracks.
    The args are searched on YouTube, so use specific search terms (e.g. artist and song title) for best results.
    """
    else ""

    val imageSection = if (hasImages) """
    The user's message includes one or more image attachments. Include this JSON command anywhere in your response so a description is saved for future context:
    {"command": "describe_image", "args": ["brief one-sentence description of the image(s)"]}
    """
    else ""

    return """
    $customPrompt
    $musicSection
    $imageSection
    Each user message will arrive with metadata in this format:
    [HH:mm] <GuildNickname> (@DiscordUsername) | #<channel>: <message>

    Example:
    [14:50] Tim (@Timothy12) | #general: how are you, bot?

    You always respond in the channel the last message came from.
    Important: do NOT echo or reproduce the metadata format in your responses.
    """.trimIndent()
}