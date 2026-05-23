# Discord Music ChatBot

A self-hosted Discord music and AI chat bot.  
It requires a connection to a [Lavalink](https://github.com/lavalink-devs/Lavalink) server for audio playback and access to an OpenAI-compatible API for AI chat responses.

## Features

- **Music playback** – play tracks and playlists from YouTube or other platforms that [Lavalink](https://github.com/lavalink-devs/Lavalink) supports
- **Music Queue Management** – queue tracks, shuffle, insert, and remove tracks from the queue
- **AI chat** – conversational responses via any OpenAI-compatible API, including the ability to queue music through natural language
- **Persistent chat history** – per-guild conversation history stored in SQLite, restored on restart
- **Configurable** – prefixes, AI model, system prompt, and more via a YAML config file

## Tech Stack

- written in [Kotlin](https://kotlinlang.org/)
- [Kord](https://github.com/kordlib/kord) – Discord API client
- [Lavalink.kt](https://github.com/kordlib/Lavalink.kt) + [Lavalink](https://github.com/lavalink-devs/Lavalink) – music search and audio playback
- [Exposed](https://github.com/JetBrains/Exposed) + SQLite – chat history persistence
- OpenAI-compatible API (e.g. [OpenAI](https://platform.openai.com/docs), [Google Gemini](https://ai.google.dev/gemini-api/docs), [LM Studio](https://lmstudio.ai)) – AI chat responses

## Prerequisites

- JDK 21+
- A [Discord bot token](https://discord.com/developers/applications)
- **For music playback:** A running and configured (preferably with [YouTube support](https://github.com/lavalink-devs/youtube-source)) [Lavalink](https://github.com/lavalink-devs/Lavalink) server
- **For AI chat:** Access to an OpenAI-compatible API (can be disabled in the config)

## Quick Start (Recommended)

Download the latest release from the GitHub Releases page.

### 1. Create the config file

Copy [`config-example.yml`](config-example.yml) to `config.yml` and fill in your credentials.

### 2. Run the bot

```bash
java -jar dmcbot.jar
```

`config.yml` is assumed to be in the working directory.  
If it's located somewhere else, you can specify the path as the first argument.

Example:

```bash
java -jar dmcbot.jar /path/to/config.yml
```

---

## Building from Source

### 1. Clone the repository

```bash
git clone https://github.com/DBrandmayr/DiscordMusicChatBot.git
cd DiscordMusicChatBot
```

### 2. Build the project

```bash
./gradlew shadowJar
```

The built JAR will be located in:

```txt
build/jars/
```

### 3. Run the built JAR

```bash
java -jar build/jars/dmcbot.jar
```
## Commands

Commands are triggered by any configured prefix (e.g. `!play lo-fi` or `hi bot play lo-fi`).  
If no prefix is configured, it defaults to `!!`.

### 🎵 Music
| Command | Aliases | Description |
|---|---|---|
| `play` | `p` | Plays a song or playlist |
| `pause` | | Pauses the current track |
| `resume` | | Resumes the current track |
| `stop` | | Stops playback and clears the queue |
| `skip` | `s` | Skips the current track |
| `playing` | `np` | Shows what's currently playing |
| `seek` | `time` | Seeks to a position in the track (`mm:ss` or seconds) |
| `replay` | `r`, `repeat` | Replays the last track (optional: number of times) |
| `volume` | `v` | Sets the volume (0–200) |
| `leave` | | Leaves the voice channel |

### 📋 Queue
| Command | Aliases | Description |
|---|---|---|
| `queue` | `q` | Shows the current queue |
| `shuffle` | `mix` | Shuffles the queue |
| `insert` | `put` | Inserts a track at a position: `insert <track> <position>` |
| `remove` | | Removes a track from the queue by position |

### 🎲 Fun
| Command | Aliases | Description |
|---|---|---|
| `random` | | Rolls a random number between two numbers |
| `coin` | `c` | Flips a coin |
| `wheel` | `w` | Spins a wheel with your options (comma-separated) |

### ℹ️ General
| Command | Aliases | Description |
|---|---|---|
| `help`  | `commands` | Shows all available commands |

## Chatbot

When a message with a valid prefix is not a command, it is forwarded to the OpenAI API. The bot maintains a per-guild conversation history (of configurable length) so it can follow the context of a conversation.

The AI can queue music on request by searching YouTube.

To disable the chat feature, set `enabled: false` under `chatbot` in the config.
