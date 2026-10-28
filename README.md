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
- GitHub Actions

## Telegram Commands

| Command | Description                                                      | Admin only |
| --- |------------------------------------------------------------------| --- |
| `/start` | Introduction and usage information                               | No |
| `/help` | List available commands                                          | No |
| `/clan` | Clan information                                                 | No |
| `/members` | Current clan members                                             | No |
| `/member <#TAG>` | Detailed clan member profile                                     | No |
| `/player <#TAG>` | Detailed player profile                                          | No |
| `/war` | Current Clan War                                                 | No |
| `/wars` | Clan War history                                                 | No |
| `/raid` | Latest raid season                                               | No |
| `/raids` | Raid season history                                              | No |
| `/raid_missed` | Members who made no raid attacks                                 | No |
| `/rating` | Overall member ratings                                           | No |
| `/rating_cw` | Clan War ratings and statistics                                  | No |
| `/rating_raids` | Raid ratings and statistics                                      | No |
| `/set_group` | Set the current group as the clan group                          | Yes |
| `/set_war_thread` | Set the current topic for Clan War notifications                 | Yes |
| `/set_main_thread` | Set the current chat as the main chat and disable commands there | Yes |
| `/reset_group` | Clear the clan group and all topic settings                      | Yes |
| `/reset_war_thread` | Clear the Clan War notification topic                            | Yes |
| `/reset_main_thread` | Clear the main chat and allow commands there again               | Yes |

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