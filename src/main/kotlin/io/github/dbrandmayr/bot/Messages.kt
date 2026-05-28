package io.github.dbrandmayr.bot

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import tools.jackson.dataformat.yaml.YAMLMapper
import tools.jackson.module.kotlin.kotlinModule
import java.io.File

@JsonIgnoreProperties(ignoreUnknown = true)
data class Messages(
    val common: CommonMessages = CommonMessages(),
    val general: GeneralMessages = GeneralMessages(),
    val music: MusicCommandsMessages = MusicCommandsMessages(),
    val queue: QueueCommandsMessages = QueueCommandsMessages(),
    @JsonProperty("fun") val funCommands: FunCommandsMessages = FunCommandsMessages()
) {
    companion object {
        var instance: Messages = Messages()
            private set

        fun load(filePath: String) {
            val file = File(filePath)
            if (!file.exists()) {
                println("No messages file found at '$filePath', using defaults.")
                return
            }
            val mapper = YAMLMapper.builder().addModule(kotlinModule()).build()
            instance = mapper.readValue(file, Messages::class.java)
            println("Messages loaded from '$filePath'.")
        }
    }
}

data class CommonMessages(
    val nothingPlaying: String = "Nothing is currently playing.",
    val queueEmpty: String = "The queue is currently empty.",
    val notInSameChannel: String = "You need to be in the same voice channel as the bot.",
    val invalidPosition: String = "Please provide a valid position number.",
    val positionOutOfRange: String = "Position is out of range for the current queue.",
    val searchNoResults: String = "No results found for \"{query}\".",
    val searchFailed: String = "Something went wrong while searching. Please try again."
)

data class GeneralMessages(
    val prompt: String = "What can I do for you?",
    val helpTitle: String = "Commands"
)

// Music

data class MusicCommandsMessages(
    val play: PlayMessages = PlayMessages(),
    val pause: PauseMessages = PauseMessages(),
    val resume: ResumeMessages = ResumeMessages(),
    val stop: StopMessages = StopMessages(),
    val leave: LeaveMessages = LeaveMessages(),
    val skip: SkipMessages = SkipMessages(),
    val playing: PlayingMessages = PlayingMessages(),
    val replay: ReplayMessages = ReplayMessages(),
    val seek: SeekMessages = SeekMessages(),
    val volume: VolumeMessages = VolumeMessages()
)

data class PlayMessages(
    val noQuery: String = "Please provide a song name or URL to play.",
    val notInVoiceChannel: String = "You need to be in a voice channel to play music.",
    val nowPlaying: String = "Now playing: \"***{title}***\"",
    val addedToQueue: String = "Added \"***{title}***\" to the queue.",
    val nowPlayingPlaylist: String = "Now playing: \"***{title}***\" and added **{count}** more tracks to the queue."
)

data class PauseMessages(
    val paused: String = "Paused."
)

data class ResumeMessages(
    val resumed: String = "Resumed."
)

data class StopMessages(
    val stopped: String = "Stopped and cleared the queue."
)

data class LeaveMessages(
    val goodbye: String = "Goodbye!"
)

data class SkipMessages(
    val skipped: String = "Skipped: \"***{title}***\""
)

data class PlayingMessages(
    val nowPlaying: String = "Now playing: \"**{title}**\"\n*{position} / {duration}*"
)

data class ReplayMessages(
    val noTrackPlayed: String = "No track has been played yet.",
    val willPlayNext: String = "\"***{title}***\" will play next.",
    val outOfRange: String = "That's out of range. Using 1 instead.",
    val invalidNumber: String = "That's not a valid number, ignoring it."
)

data class SeekMessages(
    val noTimeProvided: String = "Please provide a time to seek to.",
    val invalidTimeFormat: String = "Invalid time format. Use mm:ss or seconds.",
    val timeOutOfRange: String = "Time is out of range for the current track.",
    val seeked: String = "Seeked to **{time}**."
)

data class VolumeMessages(
    val currentVolume: String = "Current volume: **{volume}**.",
    val invalidVolume: String = "Invalid volume. Please provide a number between 0 and 200.",
    val volumeOutOfRange: String = "Volume must be between 0 and 200.",
    val volumeSet: String = "Volume set to **{volume}**."
)

//Queue

data class QueueCommandsMessages(
    val list: QueueListMessages = QueueListMessages(),
    val shuffle: ShuffleMessages = ShuffleMessages(),
    val insert: InsertMessages = InsertMessages(),
    val remove: RemoveMessages = RemoveMessages()
)

data class QueueListMessages(
    val embedTitle: String = "Queue"
)

data class ShuffleMessages(
    val shuffled: String = "Queue shuffled."
)

data class InsertMessages(
    val queueEmpty: String = "The queue is empty. Nothing to insert into.",
    val inserted: String = "Inserted **\"{title}\"** at position **{position}** in the queue.",
    val insertedPlaylist: String = "Inserted **{count}** tracks at position **{position}** in the queue."
)

data class RemoveMessages(
    val positionMustBePositive: String = "Position must be greater than 0.",
    val removed: String = "Removed \"***{title}***\" from the queue.",
    val error: String = "Something went wrong!"
)

// Fun

data class FunCommandsMessages(
    val random: RandomMessages = RandomMessages(),
    val coin: CoinMessages = CoinMessages(),
    val wheel: WheelMessages = WheelMessages()
)

data class RandomMessages(
    val invalidArgs: String = "Please provide two numbers.",
    val invalidNumbers: String = "Invalid numbers. Make sure min < max.",
    val rolling: String = "Rolling the dice...",
    val result: String = "**{number}**"
)

data class CoinMessages(
    val flipping: String = "Flipping the coin...",
    val heads: String = "Heads",
    val tails: String = "Tails",
    val result: String = "It's:\n**{result}**!"
)

data class WheelMessages(
    val notEnoughOptions: String = "Please provide at least two comma-separated options.",
    val tooManyOptions: String = "That's too many options (max 49).",
    val spinning: String = "Spinning the wheel...",
    val result: String = "# {winner}\n*{rest}*"
)