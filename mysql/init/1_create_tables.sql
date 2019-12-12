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

CREATE TABLE IF NOT EXISTS `world` (
    `name` VARCHAR(30) NOT NULL,
    `players` INT,
    `created_at` TIMESTAMP DEFAULT NOW(),
    `updated_at` TIMESTAMP DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (`name`)
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

CREATE TABLE IF NOT EXISTS `world` (
    `name` VARCHAR(30) NOT NULL,
    `players` INT,
    `created_at` TIMESTAMP DEFAULT NOW(),
    `updated_at` TIMESTAMP DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (`name`)
);
