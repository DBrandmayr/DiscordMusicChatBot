package io.github.dbrandmayr.bot.musicbot

import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.event.message.MessageCreateEvent
import io.github.dbrandmayr.bot.commands

object HelpCommand : Command {
    override val name = "help"
    override val aliases = listOf("commands")
    override val category = "ℹ️ General"
    override val description = "Shows all available commands"

    override suspend fun execute(args: List<String>, event: MessageCreateEvent) {
        event.message.channel.createEmbed {
            title = "Commands"
            commands.groupBy { it.category }.forEach { (cat, cmds) ->
                field {
                    name = "**$cat**"
                    value = cmds.joinToString("\n") { cmd ->
                        val aliasStr = if (cmd.aliases.isEmpty()) "" else " *(${cmd.aliases.joinToString(", ")})*"
                        "**`${cmd.name}`**$aliasStr — ${cmd.description}"
                    }
                    inline = false
                }
            }
        }
    }
}