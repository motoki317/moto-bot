# moto-bot

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
- `make test` to run tests. DB container needs to be launched.

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
2. Edit the bot version you want to use in `docker-compose.yml`.
    - `latest` refers to the latest version release (`v*.*.*`).
    - `master` refers to the latest commit to master branch.
    - For example, if you want to use v2.0.0 release, set the image name to `docker.pkg.github.com/motoki317/moto-bot/moto-bot/2.0.0`.
3. Create a file named `.env` and set environment variables (see `.env-sample`).
4. Execute `docker-compose up -d`.
