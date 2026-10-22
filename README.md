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

| Command | Description |
| --- | --- |
| `/start` | Introduction and usage information |
| `/help` | List available commands |
| `/clan` | Clan information |
| `/members` | Current clan roster |
| `/member <#TAG>` | Detailed clan member profile |
| `/player <#TAG>` | Detailed player profile |
| `/war` | Current Clan War |
| `/wars` | Clan War history |
| `/raid` | Latest raid season |
| `/raids` | Raid season history |
| `/raid_missed` | Members who made no raid attacks |
| `/rating` | Overall member ratings |
| `/rating_cw` | Clan War ratings and statistics |
| `/rating_raids` | Raid ratings and statistics |
| `/set_group` | Set the current group as the clan group — admin only |
| `/set_war_thread` | Set the current topic for Clan War notifications — admin only |
| `/set_main_thread` | Set the current chat as the main chat and disable commands there — admin only |
| `/reset_group` | Clear the clan group and all topic settings — admin only |
| `/reset_war_thread` | Clear the Clan War notification topic — admin only |
| `/reset_main_thread` | Clear the main chat and allow commands there again — admin only |

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

Add the bot to your group. Using an account listed in `TELEGRAM_ADMIN_USER_IDS`:

- Send `/set_group` in the group.
- Send `/set_war_thread` inside the topic intended for Clan War notifications.
- Send `/set_main_thread` in the main chat or topic used for conversation to disable bot commands there.

The main chat setting is saved in the database. Commands there receive a short notice directing users to another topic, including when sent by administrators. Administrators can still use `/set_main_thread` to change the selection or `/reset_main_thread` to clear it. Selecting a different clan group with `/set_group` clears all topic settings.

Use the reset commands in the configured clan group. `/reset_main_thread` restores commands in all topics. `/reset_war_thread` pauses Clan War notifications until a topic is set again. `/reset_group` clears the group and all its topic settings; configure the group again with `/set_group` before using clan commands. Repeating a reset when its setting is already empty returns a short notice.

Use `/help` to view the available commands.

### Updating an Existing Installation

The PostgreSQL database, user, and connection settings remain unchanged. No database rename or data migration is required.

Before moving the deployment directory, back up PostgreSQL and record the existing Docker Compose project name. Set `COMPOSE_PROJECT_NAME` to that name in the server's `.env`, then stop the existing installation with `docker compose down` without `-v`. Move the directory, including `.env`, to `/opt/coc-clan-bot` on the VPS before deploying.

Keeping the same Compose project name lets the renamed directory continue using the existing `pgdata` and `applogs` volumes. Do not remove those volumes.

### Managing the Application

**View application logs**

```bash
docker compose logs -f app
```

Press `Ctrl+C` to stop viewing logs.

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
