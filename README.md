# Discord Music ChatBot

A self-hosted, highly customizable Discord music and AI chat bot.  
It requires a connection to a [Lavalink](https://github.com/lavalink-devs/Lavalink) server for audio playback and access to an OpenAI-compatible API for AI chat responses.

## Features

- **Music playback** – play tracks and playlists from YouTube or other platforms that [Lavalink](https://github.com/lavalink-devs/Lavalink) supports
- **Music Queue Management** – queue tracks, shuffle, insert, and remove tracks from the queue
- **AI chat** – conversational responses via any OpenAI-compatible API, including the ability to queue music through natural language
- **Highly customizable** – command names and aliases, all bot response messages, prefixes, AI model, system prompt, and API endpoint — all via YAML files, no code changes needed
- **Persistent chat history** – per-guild conversation history stored in SQLite, restored on restart

## Tech Stack

- written in [Kotlin](https://kotlinlang.org/)
- [Kord](https://github.com/kordlib/kord) – Discord API client
- [Lavalink.kt](https://github.com/kordlib/Lavalink.kt) + [Lavalink](https://github.com/lavalink-devs/Lavalink) – music search and audio playback
- [Exposed](https://github.com/JetBrains/Exposed) + SQLite – chat history persistence
- OpenAI-compatible API – AI chat responses

## Prerequisites

- Java 21+
- A [Discord bot token](https://discord.com/developers/applications)
- **For music playback:** A running and configured (preferably with [YouTube support](https://github.com/lavalink-devs/youtube-source)) [Lavalink](https://github.com/lavalink-devs/Lavalink) server
- **For AI chat:** Access to an OpenAI-compatible API (e.g. [OpenAI](https://platform.openai.com/docs), [Google Gemini](https://ai.google.dev/gemini-api/docs/openai), [LM Studio](https://lmstudio.ai/docs/developer/openai-compat))

## Quick Start (Recommended)

### 1. Download the Bot JAR
Download the latest release from the [releases page](https://github.com/DBrandmayr/DiscordMusicChatBot/releases/latest).

### 2. Create the config files

Copy [`config-example.yml`](config-example.yml) to `config.yml` and fill in your credentials.  
Optionally copy [`messages-example.yml`](messages-example.yml) to `messages.yml` to customize bot messages.

### 3. Run the bot

```bash
java -jar dmcbot.jar
```

`config.yml` and `messages.yml` are assumed to be in the working directory.  
Custom paths can be passed as arguments — config first, messages second.

```bash
java -jar dmcbot.jar /path/to/config.yml /path/to/messages.yml
```

---

## Building from Source

Requires JDK 21+. No separate Gradle installation needed, the Gradle wrapper is included.

### 1. Clone the repository

```bash
git clone https://github.com/DBrandmayr/DiscordMusicChatBot.git
cd DiscordMusicChatBot
```

### 2. Build the project

**Linux / macOS**
```bash
./gradlew shadowJar
```

**Windows**
```batch
gradlew.bat shadowJar
```

The built JAR will be located in:

```
build/jars/
```

### 3. Create the config files

Copy [`config-example.yml`](config-example.yml) to `config.yml` and fill in your credentials.  
Optionally copy [`messages-example.yml`](messages-example.yml) to `messages.yml` to customize bot messages.

### 4. Run the built JAR

```bash
java -jar build/jars/dmcbot.jar
```

`config.yml` and `messages.yml` are assumed to be in the working directory.  
Custom paths can be passed as arguments, config is the first argument, messages second.

```bash
java -jar build/jars/dmcbot.jar /path/to/config.yml /path/to/messages.yml
```
## Commands

Commands are triggered by any configured prefix (e.g. `!play lo-fi` or `hi bot play lo-fi`).  
If no prefix is configured, it defaults to `!!`.

### 🎵 Music
| Command | Description |
|---|---|
| `play` | Plays a song or playlist |
| `pause` | Pauses the current track |
| `resume` | Resumes the current track |
| `stop` | Stops playback and clears the queue |
| `skip` | Skips the current track |
| `playing` | Shows what's currently playing |
| `seek` | Seeks to a position in the track (`mm:ss` or seconds) |
| `replay` | Replays the last track (optional: number of times) |
| `volume` | Sets the volume (0–200) |
| `leave` | Leaves the voice channel |

### 📋 Queue
| Command | Description |
|---|---|
| `queue` | Shows the current queue |
| `shuffle` | Shuffles the queue |
| `insert` | Inserts a track at a position: `insert <track> <position>` |
| `remove` | Removes a track from the queue by position |

### 🎲 Fun
| Command | Description |
|---|---|
| `random` | Rolls a random number between two numbers |
| `coin` | Flips a coin |
| `wheel` | Spins a wheel with your options (comma-separated) |

### ℹ️ General
| Command | Description |
|---|---|
| `help` | Shows all available commands |

## Chatbot

When a message with a valid prefix is not a command, it is forwarded to the OpenAI API. The bot maintains a per-guild conversation history (of configurable length) so it can follow the context of a conversation.

The AI can queue music on request by searching YouTube.

To disable the chat feature, set `enabled: false` under `chatbot` in the config.
