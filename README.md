# moto-bot

A discord bot for Wynncraft utility commands, written in Java using JDA.
https://forums.wynncraft.com/threads/223425/

## Usage

### Development

- `make build` to build the image and ready the bot
- `make up` to ready the bot (does not rebuild the image)
- `make down` to down the bot

For debugging
- `make db-up` to up only the db container
- `make db` to connect to db (password: `password`)

### Production

#### Environment Variables

For the bot
- `DISCORD_ACCESS_TOKEN`
- `BOT_DISCORD_ID`
- `BOT_LOG_CHANNEL_0` ~ `BOT_LOG_CHANNEL_4`
- `MYSQL_HOST`
- `MYSQL_PORT`
- `MYSQL_DATABASE`
- `MYSQL_USER`
- `MYSQL_PASSWORD`

#### Execute

With docker-compose
`docker-compose up -d`

Or directly launch the bot on the host machine
1. Prepare database
  - Database is based on MariaDB 10.4.10.
2. Execute `.sql` files in `mysql/init`.
3. Set environment variables above
4. Launch bot
```shell script
mvn clean package
./target/bin/main
```
