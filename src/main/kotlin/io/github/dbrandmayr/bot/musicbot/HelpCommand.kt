package io.github.dbrandmayr.bot.musicbot

import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.event.message.MessageCreateEvent
import io.github.dbrandmayr.bot.Messages
import io.github.dbrandmayr.bot.commands

object HelpCommand : Command {
    override val names = listOf("help", "commands")
    override val category = "ℹ️ General"
    override val description = "Shows all available commands"

    override suspend fun execute(args: List<String>, event: MessageCreateEvent) {
        event.message.channel.createEmbed {
            title = Messages.instance.general.helpTitle
            commands.groupBy { it.category }.forEach { (cat, cmds) ->
                field {
                    name = "**$cat**"
                    value = cmds.joinToString("\n") { cmd ->
                        val aliasStr = if (cmd.names.size <= 1) "" else " *(${cmd.names.drop(1).joinToString(", ")})*"
                        "**`${cmd.names.first()}`**$aliasStr — ${cmd.description}"
                    }
                    inline = false
                }
            }
        }
    }
}