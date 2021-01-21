# moto-bot

[![](https://github.com/motoki317/moto-bot/workflows/Build%20and%20Test/badge.svg)](https://github.com/motoki317/moto-bot/actions?query=workflow%3A%22Build+and+Test%22)
[![](https://github.com/motoki317/moto-bot/workflows/Release%20master/badge.svg)](https://github.com/motoki317/moto-bot/actions?query=workflow%3A%22Release+master%22)
[![](https://github.com/motoki317/moto-bot/workflows/Release%20version/badge.svg)](https://github.com/motoki317/moto-bot/actions?query=workflow%3A%22Release+version%22)

A discord bot for Wynncraft utility commands, written in Java using JDA.

See usage (old v1 info):
https://forums.wynncraft.com/threads/223425/

## Development

Some useful shortcuts for development are written in `Makefile`.

- `make build` to (re-)build the image and launch the bot.
- `make up` to launch the bot (does not rebuild the image).
- `make down` to stop the bot and DB.

For debugging:
- `make db-up` to launch only the DB container.
- `make db` to connect to DB (password: `password`).
- `mvn test` to run tests. DB container needs to be launched.

## Production

You can either manually build and install the bot, or pull image from the release.
Using docker might be easier but overheads could be a problem in small servers.

### Manual Installation

Manual build and installation (does not use docker)

1. Clone this repository.
2. Install correct version of MariaDB (see `docker-compose.yaml` at root).
3. Execute sql files in `mysql/init` directory.
4. Set these environment variables for the bot.
    - `DISCORD_ACCESS_TOKEN` ... Discord bot account access token
    - `BOT_DISCORD_ID` ... Bot's discord user ID
    - `BOT_LOG_CHANNEL_0` ~ `BOT_LOG_CHANNEL_4` ... Discord channel IDs to which bot sends logs
    - `PLAYER_TRACKER_CHANNEL` ... Discord channel ID to which bot sends player number logs once a day
    - `BOT_RESTART_CHANNEL` ... Discord channel ID to which bot sends a log on every restart
    - `MYSQL_HOST` ... MariaDB host name
    - `MYSQL_PORT` ... MariaDB port
    - `MYSQL_DATABASE` ... MariaDB database name
    - `MYSQL_USER` ... MariaDB username
    - `MYSQL_PASSWORD` ... MariaDB password for the given username
5. Install maven (see `Dockerfile` at root for version).
6. Build, and launch the bot.
```shell script
mvn clean package -D skipTests
./target/bin/main
```

### Docker Installation

1. Clone this repository, or copy the root `docker-compose.yaml` and `mysql/init` directory since only these files are required.
2. Create a file named `.env` and set environment variables (see `.env-sample`).
3. Execute `docker-compose up -d`.
