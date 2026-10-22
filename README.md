# Gucci Goblin

This repository contains the source code for a Telegram bot created to process data from official [Clash of Clans API](https://developer.clashofclans.com/).

## Features

- Persistent storage of collected data over time
- Automatic data synchronization with the Clash of Clans API every minute
- Cumulative tracking of troops donated and received across seasons
- Clan membership tracking, including departures and returns
- Clan War history, including participants, attacks, and results
- Raid season history, including participation, attacks, and Capital Gold earned
- Tracking of missed raid attacks
- Clan War notifications and attack reminders
- Rating: member performance relative to other clan members, with Overall, War, and Raid ratings.
- Administrator-controlled Telegram group and notification topic settings
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
| `/raid_missed` | Members who missed raid attacks |
| `/rating` | Overall member ratings |
| `/rating_cw` | Clan War ratings and statistics |
| `/rating_raids` | Raid ratings and statistics |
| `/set_group` | Set the current group as the clan group — admin only |
| `/war_thread` | Set the current topic for Clan War notifications — admin only |

## Running
