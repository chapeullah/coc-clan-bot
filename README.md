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
- Administrator-managed links between clan members and Telegram users
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
| `/link_member <#TAG>` | Link a current clan member to the author of the replied-to message — admin only |
| `/unlink_member <#TAG>` | Delete a member's Telegram link — admin only |
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

Use `/help` to view the available commands.

To link a clan member, reply to that person's message with `/link_member #TAG`.
The bot saves the author's numeric Telegram user ID, so a Telegram username is not required.
Use `/unlink_member #TAG` to remove the link; replying to a message is not required for removal.
Remove an existing link before assigning the same game account to another Telegram user.

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
