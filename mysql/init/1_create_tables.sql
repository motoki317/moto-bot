CREATE DATABASE IF NOT EXISTS `moto-bot`;

USE `moto-bot`;

CREATE TABLE IF NOT EXISTS `track_channel` (
    `type` VARCHAR(30) NOT NULL,
    `guild_id` BIGINT NOT NULL,
    `channel_id` BIGINT NOT NULL,
    `guild_name` VARCHAR(30) NULL,
    `player_name` VARCHAR(16) NULL,
    `guild_name_v` VARCHAR(30) AS (IF(`guild_name` IS NULL, '', `guild_name`)) VIRTUAL,
    `player_name_v` VARCHAR(16) AS (IF(`player_name` IS NULL, '', `player_name`)) VIRTUAL,
    UNIQUE KEY (`type`, `guild_id`, `channel_id`, `guild_name_v`, `player_name_v`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `world` (
    `name` VARCHAR(30) NOT NULL,
    `players` INT NOT NULL,
    `created_at` TIMESTAMP DEFAULT NOW(),
    `updated_at` TIMESTAMP DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `command_log` (
    `id` INT AUTO_INCREMENT NOT NULL,
    `kind` VARCHAR(30) NOT NULL,
    `full` VARCHAR(2500) NOT NULL,
    `user_id` BIGINT NOT NULL,
    `dm` BOOLEAN NOT NULL,
    PRIMARY KEY (`id`),
    KEY `kind_idx` (`kind`),
    KEY `user_id_idx` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

# -- Test --
DROP DATABASE IF EXISTS `moto-bot_test`;
CREATE DATABASE `moto-bot_test`;

USE `moto-bot_test`;

CREATE TABLE IF NOT EXISTS `track_channel` (
    `type` VARCHAR(30) NOT NULL,
    `guild_id` BIGINT NOT NULL,
    `channel_id` BIGINT NOT NULL,
    `guild_name` VARCHAR(30) NULL,
    `player_name` VARCHAR(16) NULL,
    `guild_name_v` VARCHAR(30) AS (IF(`guild_name` IS NULL, '', `guild_name`)) VIRTUAL,
    `player_name_v` VARCHAR(16) AS (IF(`player_name` IS NULL, '', `player_name`)) VIRTUAL,
    UNIQUE KEY (`type`, `guild_id`, `channel_id`, `guild_name_v`, `player_name_v`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `world` (
    `name` VARCHAR(30) NOT NULL,
    `players` INT NOT NULL,
    `created_at` TIMESTAMP DEFAULT NOW(),
    `updated_at` TIMESTAMP DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `command_log` (
    `id` INT AUTO_INCREMENT NOT NULL,
    `kind` VARCHAR(30) NOT NULL,
    `full` VARCHAR(2500) NOT NULL,
    `user_id` BIGINT NOT NULL,
    `dm` BOOLEAN NOT NULL,
    PRIMARY KEY (`id`),
    KEY `kind_idx` (`kind`),
    KEY `user_id_idx` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
