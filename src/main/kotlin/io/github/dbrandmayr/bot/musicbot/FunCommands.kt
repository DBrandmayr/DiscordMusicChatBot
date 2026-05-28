package io.github.dbrandmayr.bot.musicbot

import dev.kord.core.event.message.MessageCreateEvent
import io.github.dbrandmayr.bot.Messages
import io.github.dbrandmayr.bot.fill
import kotlinx.coroutines.delay
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

private const val CATEGORY = "🎲 Fun Commands"

object RandomCommand : Command {
    override val names = listOf("random")
    override val category = CATEGORY
    override val description = "Rolls a random number between two numbers"

    override suspend fun execute(args: List<String>, event: MessageCreateEvent) {
        val channel = event.message.channel
        val arguments = args.map { it.replace(",", "").trim() }.filterNot { it.isBlank() }
        val min = arguments.firstOrNull()?.toIntOrNull()
        val max = arguments.lastOrNull()?.toIntOrNull()

        if (arguments.count() != 2) {
            channel.createMessage(Messages.instance.funCommands.random.invalidArgs)
        } else if (min == null || max == null || min >= max) {
            channel.createMessage(Messages.instance.funCommands.random.invalidNumbers)
        } else {
            channel.createMessage(Messages.instance.funCommands.random.rolling)
            delay(1900.milliseconds)
            channel.createMessage(Messages.instance.funCommands.random.result.fill("number" to (min..max).random().toString()))
        }
    }
}

object CoinCommand : Command {
    override val names = listOf("coin", "c")
    override val category = CATEGORY
    override val description = "Flips a coin (Heads or Tails)"

    override suspend fun execute(args: List<String>, event: MessageCreateEvent) {
        val channel = event.message.channel
        channel.createMessage(Messages.instance.funCommands.coin.flipping)
        delay(1900.milliseconds)
        val result = if (Random.nextBoolean()) Messages.instance.funCommands.coin.heads else Messages.instance.funCommands.coin.tails
        channel.createMessage(Messages.instance.funCommands.coin.result.fill("result" to result))
    }
}

object WheelCommand : Command {
    override val names = listOf("wheel", "w")
    override val category = CATEGORY
    override val description = "Spins a wheel with your options (comma-separated)"

    override suspend fun execute(args: List<String>, event: MessageCreateEvent) {
        val channel = event.message.channel
        val wheelOptions = args.joinToString(" ").split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val wheelCount = wheelOptions.count()

        if (wheelCount <= 1) {
            channel.createMessage(Messages.instance.funCommands.wheel.notEnoughOptions)
            return
        } else if (wheelCount >= 50) {
            channel.createMessage(Messages.instance.funCommands.wheel.tooManyOptions)
            return
        }

        val randomIndex = (0..<wheelCount).random()
        channel.createMessage(Messages.instance.funCommands.wheel.spinning)
        val choice = wheelOptions[randomIndex]
        val rest = wheelOptions.toMutableList().apply { removeAt(randomIndex) }.joinToString(", ")
        delay(2200.milliseconds)
        channel.createMessage(Messages.instance.funCommands.wheel.result.fill("winner" to choice, "rest" to rest.trim()))
    }
}

val funCommands: List<Command> = listOf(RandomCommand, CoinCommand, WheelCommand)