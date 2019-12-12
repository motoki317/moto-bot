CREATE DATABASE IF NOT EXISTS `moto-bot`;

USE `moto-bot`;

CREATE TABLE IF NOT EXISTS `track_channel` (
    `type` VARCHAR(30) NOT NULL,
    `guild_id` BIGINT NOT NULL,
    `channel_id` BIGINT NOT NULL,
    `guild_name` VARCHAR(30),
    `player_name` VARCHAR(16),
    PRIMARY KEY (`type`, `guild_id`, `channel_id`)
);

# -- Test --
DROP DATABASE IF EXISTS `moto-bot_test`;
CREATE DATABASE `moto-bot_test`;

USE `moto-bot_test`;

CREATE TABLE IF NOT EXISTS `track_channel` (
   `type` VARCHAR(30) NOT NULL,
   `guild_id` BIGINT NOT NULL,
   `channel_id` BIGINT NOT NULL,
   `guild_name` VARCHAR(30),
   `player_name` VARCHAR(16),
   PRIMARY KEY (`type`, `guild_id`, `channel_id`)
);
