# CoC Clan Bot

This repository contains the source code for a Telegram bot created to process data from the official [Clash of Clans API](https://developer.clashofclans.com/).

## Features

- Persistent storage of collected data over time
- Automatic data synchronization with the Clash of Clans API every minute
- Cumulative tracking of troops donated and received across seasons
- Clan membership tracking, including departures and returns
- Clan War history, including participants, attacks, and results
- Raid season history, including participation, attacks, and Capital Gold earned
- Tracking of missed raid attacks
- Clan War notifications and attack reminders
- Rating: member performance relative to other clan members, with Overall, War, and Raid ratings
- Administrator-controlled Telegram group and notification topic settings
- Commands can be disabled in the main chat while remaining available in other topics
- Private replies to commands in group chats

## Built With

- Java 21
- Spring Boot 4.0.2
- Spring Data JPA
- PostgreSQL 16
- Lombok
- Maven
- Docker Compose
- CI/CD — GitHub Actions

## Telegram Commands

| Command | Description | Admin only |
| --- | --- | --- |
| `/start` | Introduction and usage information | No |
| `/help` | List available commands | No |
| `/clan` | Clan information | No |
| `/members` | Current clan roster | No |
| `/member <#TAG>` | Detailed clan member profile | No |
| `/player <#TAG>` | Detailed player profile | No |
| `/war` | Current Clan War | No |
| `/wars` | Clan War history | No |
| `/raid` | Latest raid season | No |
| `/raids` | Raid season history | No |
| `/raid_missed` | Members who made no raid attacks | No |
| `/rating` | Overall member ratings | No |
| `/rating_cw` | Clan War ratings and statistics | No |
| `/rating_raids` | Raid ratings and statistics | No |
| `/set_group` | Set the current group as the clan group | Yes |
| `/set_war_thread` | Set the current topic for Clan War notifications | Yes |
| `/set_main_thread` | Set the current chat as the main chat and disable commands there | Yes |
| `/reset_group` | Clear the clan group and all topic settings | Yes |
| `/reset_war_thread` | Clear the Clan War notification topic | Yes |
| `/reset_main_thread` | Clear the main chat and allow commands there again | Yes |

## Environment Variables

Copy `.env.example` to `.env` in the project root and replace the example values with your own.

| Variable | Description | Required |
| --- | --- | --- |
| `COC_API_BASE_URL` | Clash of Clans API base URL | Yes |
| `COC_API_TOKEN` | Clash of Clans API token | Yes |
| `COC_CLAN_TAG` | Clan tag to monitor | Yes |
| `COC_API_RESPONSE_TIMEOUT_MS` | Synchronization time limit in milliseconds | Yes |
| `TELEGRAM_BOT_TOKEN` | Telegram bot token | Yes |
| `TELEGRAM_SYSTEM_USER_ID` | Positive Telegram user ID for system notifications in a private chat | Yes |
| `TELEGRAM_ADMIN_USER_IDS` | Comma-separated administrator user IDs | Yes |
| `TELEGRAM_BOT_POLL_DELAY_MS` | Polling delay in milliseconds; defaults to 1000 | No |

For `TELEGRAM_SYSTEM_USER_ID`, enter the numeric user ID of the system notification recipient. The recipient should start a private chat with the bot by sending `/start` before receiving notifications.

## Running

Requires **Git** and **Docker with Docker Compose**.

**1. Clone the repository**

```bash
git clone https://github.com/chapeullah/coc-clan-bot.git
```

**2. Go to the project directory**

```bash
cd coc-clan-bot
```

**3. Configure environment variables**

See the [Environment Variables](#environment-variables) section for configuration instructions.

**4. Build and run**

```bash
docker compose up -d --build
```

**5. Configure the Telegram group**

Add the bot to your group and grant it administrator rights so private command
replies in the group can still be delivered when the original reply target has
expired or been deleted. Administrative commands are excluded from shared menus.
A personal menu is refreshed when a user sends a command: configured bot
administrators receive administrative commands, while other users receive only
the commands available to them. Private chats allow only `/start` and `/help`,
including for administrators. Clan commands are available only in the configured
group; administrators can use `/set_group` in a group to configure it.

Using an account listed in `TELEGRAM_ADMIN_USER_IDS`:

- Send `/set_group` in the group.
- Send `/set_war_thread` inside the topic intended for Clan War notifications.
- Send `/set_main_thread` in the main chat or topic used for conversation to disable bot commands there.

Use `/help` to view the available commands.

### Managing the Application

**View application logs**

```bash
docker compose logs -f app
```

Press `Ctrl+C` to stop viewing logs.

HTTP failures include the operation, sanitized endpoint, attempt number, duration,
configured timeouts, response status and selected response headers, response body
read progress, error category and exception cause chain. Diagnostic snippets of
failed response bodies are taken from at most 2 KiB, sanitized and bounded to
2048 characters (plus a truncation marker); the HTTP response itself is unchanged.
A successful retry produces an INFO recovery
entry; ordinary successful HTTP requests are logged at DEBUG.

Use `syncId` to follow one scheduled synchronization and `requestId` to follow all
attempts of one logical HTTP request. A synchronization deadline records the stage
and last recorded request when cancellation was triggered, retaining the original cause.
Body progress counts observed reads; `responseBodyEndObserved=false` can also occur
on success when the converter reads exactly Content-Length without probing for EOF.
`HTTP_TIMEOUT` identifies a client timeout; it does not identify which server or
network component caused the delay. `SYNC_DEADLINE` identifies local cancellation.
DNS, connection, TLS, response body I/O, HTTP status and response decoding failures
have separate categories. Suppressed exceptions remain available in the final
stack trace. With Spring's JDK request factory, a body read timeout can surface as
an I/O error from a closed stream without a timeout cause; it is reported as
`RESPONSE_BODY_IO`, without guessing why the stream closed.

For temporary transport diagnostics, set `COC_HTTP_TRANSPORT_LOG=errors,ssl,channel`
in `.env` and recreate the app container so the JVM receives the setting at startup.
This enables JDK transport logging for both CoC and Telegram HTTP clients. Leave
the value empty for normal operation. Request headers and response contents are
not enabled by this diagnostic preset. JDK events reach Logback through Spring
Boot's JUL bridge. Their worker threads do not inherit the application's `syncId`
or `requestId`; use the application HTTP entries for request correlation.
Application console and file logging mask configured API tokens, Telegram bot
URLs, Bearer credentials and Authorization values. Message and exception control
characters are escaped while stack frames retain their line breaks.

Archived application logs are retained for up to 365 days, subject to the 1 GB
archive limit. The active log rotates at a 10 MB threshold before compression.

**Stop the application**

```bash
docker compose stop
```

**Start the application**

```bash
docker compose start
```

## CI/CD

The repository includes a GitHub Actions workflow that deploys the application to a VPS on pushes to `main` or through a manual workflow run.
