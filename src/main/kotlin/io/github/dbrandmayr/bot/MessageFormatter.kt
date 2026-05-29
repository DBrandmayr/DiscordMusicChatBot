package io.github.dbrandmayr.bot

fun formatDuration(ms: Long): String = "%02d:%02d".format(ms / 1000 / 60, ms / 1000 % 60)

fun String.fill(vararg pairs: Pair<String, String>): String =
    pairs.fold(this) { acc, (key, value) -> acc.replace("{$key}", value) }

fun formatAddedTracks(tracks: List<Pair<String, Long>>): String {
    val reset = "[0m"
    val green = "[1;32m"
    val gray = "[2;37m"

    val lines = tracks.joinToString("\n") { (title, durationMs) ->
        "${green}[+]${reset} $title ${gray}• ${formatDuration(durationMs)}${reset}"
    }
    return "```ansi\n$lines\n```"
}
