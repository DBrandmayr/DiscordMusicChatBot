package io.github.dbrandmayr.bot.musicbot

import dev.kord.core.event.message.MessageCreateEvent
import kotlinx.coroutines.delay
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

private const val CATEGORY = "🎲 Fun Commands"

object RandomCommand : Command {
    override val name = "random"
    override val category = CATEGORY
    override val description = "Rolls a random number between two numbers"

    override suspend fun execute(args: List<String>, event: MessageCreateEvent) {
        val channel = event.message.channel
        val arguments = args.map { it.replace(",", "").trim() }.filterNot { it.isBlank() }
        val min = arguments.firstOrNull()?.toIntOrNull()
        val max = arguments.lastOrNull()?.toIntOrNull()

        if (arguments.count() != 2) {
            channel.createMessage("Please provide two numbers.")
        } else if (min == null || max == null || min >= max) {
            channel.createMessage("Invalid numbers. Make sure min < max.")
        } else {
            channel.createMessage("Rolling the dice...")
            delay(1900.milliseconds)
            channel.createMessage("**${(min..max).random()}**")
        }
    }
}

object CoinCommand : Command {
    override val name = "coin"
    override val aliases = listOf("c")
    override val category = CATEGORY
    override val description = "Flips a coin (Heads or Tails)"

    override suspend fun execute(args: List<String>, event: MessageCreateEvent) {
        val channel = event.message.channel
        channel.createMessage("Flipping the coin...")
        delay(1900.milliseconds)
        val result = if (Random.nextBoolean()) "Heads" else "Tails"
        channel.createMessage("It's:\n**${result}**!")
    }
}

object WheelCommand : Command {
    override val name = "wheel"
    override val aliases = listOf("w")
    override val category = CATEGORY
    override val description = "Spins a wheel with your options (comma-separated)"

    override suspend fun execute(args: List<String>, event: MessageCreateEvent) {
        val channel = event.message.channel
        val wheelOptions = args.joinToString(" ").split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val wheelCount = wheelOptions.count()

        if (wheelCount <= 1) {
            channel.createMessage("Please provide at least two comma-separated options.")
            return
        } else if (wheelCount >= 50) {
            channel.createMessage("That's too many options (max 49).")
            return
        }

        val randomIndex = (0..<wheelCount).random()
        channel.createMessage("Spinning the wheel...")
        val choice = wheelOptions[randomIndex]
        val rest = wheelOptions.toMutableList().apply { removeAt(randomIndex) }.joinToString(", ")
        delay(2200.milliseconds)
        channel.createMessage("# $choice \n*${rest.trim()}*")
    }
}

val funCommands: List<Command> = listOf(RandomCommand, CoinCommand, WheelCommand)