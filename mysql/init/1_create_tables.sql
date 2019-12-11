CREATE DATABASE IF NOT EXISTS `moto-bot`;

USE `moto-bot`;

CREATE TABLE IF NOT EXISTS `tracking_channels` (
    `guild_id` BIGINT NOT NULL,
    `channel_id` BIGINT NOT NULL,
    `type` VARCHAR(30) NOT NULL,
    PRIMARY KEY (`type`, `guild_id`, `channel_id`)
);

# -- Test --
DROP DATABASE IF EXISTS `moto-bot_test`;
CREATE DATABASE `moto-bot_test`;

USE `moto-bot_test`;

CREATE TABLE IF NOT EXISTS `tracking_channels` (
   `guild_id` BIGINT NOT NULL,
   `channel_id` BIGINT NOT NULL,
   `type` VARCHAR(30) NOT NULL,
   PRIMARY KEY (`type`, `guild_id`, `channel_id`)
);
