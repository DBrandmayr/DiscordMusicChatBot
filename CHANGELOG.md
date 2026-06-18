# Changelog

## [v0.5.0] - 2026-06-19

### Breaking Changes

Renamed `command-names` config key to camelCase `commandNames`.

See the updated [`config-example.yml`](config-example.yml) for the full new structure.

---

### New Features

#### Web Search via SearXNG
The chatbot can now search the web to answer questions about current events, facts, or anything outside its training data.

- Requires a self-hosted [SearXNG](https://github.com/searxng/searxng) instance
- Configure the endpoint under `chatbot.searxng` in your config
- When the bot performs a search, a notice is shown in the Discord channel
- The bot decides autonomously when a search would improve its answer

#### Configurable Chatbot Temperature
A new `temperature` option is available under `chatbot.openai` to control how creative or deterministic the AI's responses are. Lower values (e.g. `0.2`) are more focused; higher values (e.g. `1.0`) are more varied.

---

### Bug Fixes

- Fixed music queue stalling indefinitely when the Lavalink event stream drops unexpectedly
- Fixed a long-standing issue where the bot would report "nothing playing" even while a track was active, the current track is now fetched live from the Lavalink node instead of being cached

---

## [v0.4.0] - 2026-06-02

### Breaking Changes

**Config file structure has changed.** The `lavalink` block is now nested under a `music` root node, and the OpenAI settings are now nested under a `chatbot` root node. You must update your `config.yml` before upgrading.

Before:
```yaml
lavalink:
  host: "localhost"
  ...
openai:
  key: "KEY_HERE"
  ...
```

After:
```yaml
music:
  lavalink:
    host: "localhost"
    ...

chatbot:
  openai:
    key: "KEY_HERE"
    ...
```

See the updated [`config-example.yml`](config-example.yml) for the full new structure.

---

### New Features

#### Image Attachments in Chat
The chatbot can now see and respond to images. Attach an image to your message and the bot will describe and discuss it.

- Images attached to messages are passed to the AI for analysis
- Image descriptions are stored in the persistent chat history, so the bot remembers what it saw even after the chat context is refreshed
- Add `allowImages: false` under `chatbot` in your config to disable image processing entirely
- Add `useBase64Images: true` under `chatbot.openai` if your API requires Base64-encoded images (e.g. LM Studio)

#### SSL/TLS Support for Lavalink
Secure Lavalink connections are now supported. Set `secure: true` under `music.lavalink` in your config to enable SSL.

#### Configurable API Timeout
A new `timeoutSeconds` option is available under `chatbot.openai` to control how long the bot waits for the AI API to respond before timing out. Useful when using slower local models.

---

## [v0.3.1]

### Bug Fixes

- Fixed a regression where chatting with the bot in a guild not yet stored in the database caused the bot to not respond and not save the message to the database.

---

## [v0.3.0]

### New Features

#### Customizable Bot Messages
All messages the bot sends are now configurable via a `messages.yml` file. Copy [`messages-example.yml`](messages-example.yml), adjust any responses you want to change, and the rest fall back to the built-in defaults. Supported: music responses, queue feedback, fun command output including coin flip sides and wheel result format, and more.

#### Configurable Command Names and Aliases
Command names and aliases can now be changed in `config.yml` under `command-names`. Every command accepts a list of names — the first is the display name shown in the help embed, the rest all trigger the command equally. Useful for renaming commands to another language or adding your own shortcuts.

#### Bot Online Notification
The console now prints a clearly visible message once the bot has successfully connected to Discord, so it's obvious when the bot is ready.

### Bug Fixes

- Fixed the AI play command showing the wrong error message when a track failed to load.

### Internal

- Simplified the command name/alias system from a separate name + aliases pair to a single flat names list.
- Added a startup warning when duplicate command names are detected across commands.

---

## [v0.2.0]

### New Features

#### OpenAI-Compatible API Support
The bot is no longer tied to OpenAI. You can now use any OpenAI-compatible API by setting `completionsUrl` in the config. Examples are provided in [`config-example.yml`](config-example.yml).

#### Improved AI Music Queuing
When the AI queues a song via natural language, the song title is now shown in the bot's response.

### Bug Fixes

- Fixed chat history being polluted on API errors. Previously, if the AI API returned an error, the user message was still added to the chat history with no reply, sometimes leading to confusing follow-up answers. Failed requests are now fully discarded.

### Internal

- Extracted Lavalink connection and guild music management into `LavalinkManager`.

---

## [v0.1.0]

Initial release.
