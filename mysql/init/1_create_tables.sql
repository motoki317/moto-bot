CREATE DATABASE IF NOT EXISTS `moto-bot`;

USE `moto-bot`;

CREATE TABLE IF NOT EXISTS `track_channel` (
    `type` VARCHAR(30) NOT NULL,
    `guild_id` BIGINT NOT NULL,
    `channel_id` BIGINT NOT NULL,
    `guild_name` VARCHAR(30) NULL,
    `player_name` VARCHAR(16) NULL,
    # discord user id of the user who created track
    `user_id` BIGINT NOT NULL,
    `expires_at` DATETIME NOT NULL,
    `guild_name_v` VARCHAR(30) AS (IF(`guild_name` IS NULL, '', `guild_name`)) VIRTUAL,
    `player_name_v` VARCHAR(16) AS (IF(`player_name` IS NULL, '', `player_name`)) VIRTUAL,
    UNIQUE KEY (`type`, `guild_id`, `channel_id`, `guild_name_v`, `player_name_v`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `world` (
    `name` VARCHAR(30) NOT NULL,
    `players` INT NOT NULL,
    `created_at` DATETIME DEFAULT NOW(),
    `updated_at` DATETIME DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `command_log` (
    `id` INT AUTO_INCREMENT PRIMARY KEY NOT NULL,
    `kind` VARCHAR(30) NOT NULL,
    `full` VARCHAR(2500) NOT NULL,
    `guild_id` BIGINT NULL,
    `channel_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT NOW(),
    KEY `kind_idx` (`kind`),
    KEY `user_id_idx` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `guild` (
    # Guild name case sensitive and distinguishes trailing spaces
    `name` VARBINARY(30) PRIMARY KEY NOT NULL,
    `prefix` VARCHAR(5) NOT NULL,
    `created_at` DATETIME NOT NULL,
    # For case insensitive & ignoring trailing space search
    `varchar_name` VARCHAR(30) AS (`name`) PERSISTENT,
    KEY `varchar_name_idx` (`varchar_name`),
    KEY `prefix_idx` (`prefix`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

# Stores data retrieved from Wynncraft API
CREATE TABLE IF NOT EXISTS `guild_leaderboard` (
    `name` VARBINARY(30) NOT NULL,
    `prefix` VARCHAR(5) NOT NULL,
    `xp` BIGINT NOT NULL,
    `level` INT NOT NULL,
    `num` INT NOT NULL,
    `territories` INT NOT NULL,
    `member_count` INT NOT NULL,
    `updated_at` DATETIME NOT NULL,
    UNIQUE KEY `updated_at_name_idx` (`updated_at`, `name`),
    KEY `name_idx` (`name`, `updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

# To be generated from `guild_leaderboard` table on update
CREATE TABLE IF NOT EXISTS `guild_xp_leaderboard` (
    `name` VARBINARY(30) PRIMARY KEY NOT NULL,
    `prefix` VARCHAR(5) NOT NULL,
    `level` INT NOT NULL,
    `xp` BIGINT NOT NULL,
    `xp_diff` BIGINT NOT NULL,
    `from` DATETIME NOT NULL,
    `to` DATETIME NOT NULL,
    KEY `xp_diff_idx` (`xp_diff`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `territory` (
    `name` VARCHAR(100) PRIMARY KEY NOT NULL,
    `guild_name` VARCHAR(30),
    `acquired` DATETIME,
    `attacker` VARCHAR(30) NULL,
    `start_x` INT,
    `start_z` INT,
    `end_x` INT,
    `end_z` INT,
    KEY `guild_idx` (`guild_name`),
    KEY `acquired_idx` (`acquired`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

# Territory log is to be automatically updated by triggers, on `territory` table update
CREATE TABLE IF NOT EXISTS `territory_log` (
    `id` INT AUTO_INCREMENT PRIMARY KEY NOT NULL,
    `territory_name` VARCHAR(100) NOT NULL,
    `old_guild_name` VARCHAR(30),
    `new_guild_name` VARCHAR(30),
    `old_guild_terr_amt` INT NOT NULL,
    `new_guild_terr_amt` INT NOT NULL,
    `acquired` DATETIME NOT NULL DEFAULT NOW(),
    # milliseconds
    `time_diff` BIGINT NOT NULL,
    KEY `territory_name_idx` (`territory_name`, `acquired` DESC),
    KEY `old_guild_idx` (`old_guild_name`, `acquired` DESC),
    KEY `new_guild_idx` (`new_guild_name`, `acquired` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

# War log table is to be updated in the code
CREATE TABLE IF NOT EXISTS `war_log` (
    `id` INT AUTO_INCREMENT PRIMARY KEY NOT NULL,
    `server_name` VARCHAR(10) NOT NULL,
    `guild_name` VARCHAR(30) NULL,
    `created_at` DATETIME NOT NULL DEFAULT NOW(),
    `last_up` DATETIME NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    `ended` BOOLEAN NOT NULL,
    # boolean indicating if the player tracker has sent log of this log
    `log_ended` BOOLEAN NOT NULL,
    KEY `guild_idx` (`guild_name`, `created_at` DESC),
    KEY `ended_idx` (`ended`),
    KEY `log_ended_idx` (`log_ended`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

# Associates with war_log table to keep track of players that joined a war server
CREATE TABLE IF NOT EXISTS `war_player` (
    `war_log_id` INT NOT NULL,
    `player_name` VARCHAR(30) NOT NULL,
    # player mc uuid with hyphens
    `player_uuid` CHAR(36) NULL,
    # A flag indicating player left war server before the war server itself ends,
    # or that guild acquired a territory (= `ended` flag in `war_log` table)
    `exited` BOOLEAN NOT NULL,
    PRIMARY KEY (`war_log_id`, `player_name`),
    KEY `name_id_idx` (`player_name`, `war_log_id`),
    KEY `uuid_id_idx` (`player_uuid`, `war_log_id`),
    KEY `uuid_exited_id_idx` (`player_uuid`, `exited`, `war_log_id`),
    CONSTRAINT `fk_war_log_id` FOREIGN KEY (`war_log_id`) REFERENCES `war_log` (`id`)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

# Keep track of messages sent to discord for war tracking
CREATE TABLE IF NOT EXISTS `war_track` (
    `war_log_id` INT NOT NULL,
    `discord_channel_id` BIGINT NOT NULL,
    `discord_message_id` BIGINT NOT NULL,
    PRIMARY KEY (`war_log_id`, `discord_channel_id`),
    CONSTRAINT `fk_war_track_log_id` FOREIGN KEY (`war_log_id`) REFERENCES `war_log` (`id`)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

# A table that aggregates `war_log` / `territory_log` for a guild.
# Records all wars (<-> associated if possible), acquire territory, lost territory (never associated with a war log)
CREATE TABLE IF NOT EXISTS `guild_war_log` (
    `id` INT AUTO_INCREMENT PRIMARY KEY NOT NULL,
    `guild_name` VARCHAR(30) NOT NULL,
    `war_log_id` INT NULL,
    `territory_log_id` INT NULL,
    UNIQUE KEY `guild_idx` (`guild_name`, `id` DESC),
    UNIQUE KEY `guild_war_territory_log_idx` (`guild_name`, `war_log_id`, `territory_log_id`),
    UNIQUE KEY `war_territory_log_idx` (`war_log_id`, `territory_log_id`),
    UNIQUE KEY `territory_log_idx` (`territory_log_id`, `guild_name`),
    CONSTRAINT `fk_guild_war_log_id` FOREIGN KEY (`war_log_id`) REFERENCES `war_log` (`id`)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT `fk_guild_territory_log_id` FOREIGN KEY (`territory_log_id`) REFERENCES `territory_log` (`id`)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

# Player war leaderboard, to be updated on `war_player` update
CREATE TABLE IF NOT EXISTS `player_war_leaderboard` (
    `uuid` CHAR(36) PRIMARY KEY NOT NULL,
    `last_name` VARCHAR(30) NOT NULL,
    `total_war` INT NOT NULL,
    `success_war` INT NOT NULL,
    `survived_war` INT NOT NULL,
    `success_rate` DECIMAL(5,4) UNSIGNED AS (success_war / total_war) PERSISTENT,
    `survived_rate` DECIMAL(5,4) UNSIGNED AS (survived_war / total_war) PERSISTENT,
    KEY `total_idx` (`total_war`),
    KEY `success_idx` (`success_war`),
    KEY `survived_idx` (`survived_war`),
    KEY `success_rate_idx` (`success_rate`),
    KEY `survived_rate_idx` (`survived_rate`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP FUNCTION IF EXISTS `count_guild_territories`;
DELIMITER //
CREATE FUNCTION `count_guild_territories` (g_name VARCHAR(30)) RETURNS INT
    BEGIN
        RETURN (SELECT COUNT(*) FROM `territory` WHERE `guild_name` = g_name);
    END; //
DELIMITER ;

# On `territory` table, if `guild` column (owner of the territory) was updated... insert into `territory_log` table
DELIMITER //
CREATE TRIGGER IF NOT EXISTS `territory_logger`
    AFTER UPDATE ON `territory` FOR EACH ROW
    BEGIN
        # on guild column update
        IF IF(NEW.guild_name <=> OLD.guild_name, 0, 1) THEN
            INSERT INTO `territory_log`
                (territory_name, old_guild_name, new_guild_name, old_guild_terr_amt, new_guild_terr_amt, acquired, time_diff)
                VALUES (NEW.name, OLD.guild_name, NEW.guild_name,
                        count_guild_territories(OLD.guild_name), count_guild_territories(NEW.guild_name),
                        NEW.acquired, (UNIX_TIMESTAMP(NEW.acquired) - UNIX_TIMESTAMP(OLD.acquired)) * 1000);
        END IF;
    END; //
DELIMITER ;

# Selects id of the last war log for guild that is not yet associated to an territory log
DROP FUNCTION IF EXISTS `last_unassociated_war_log_id`;
DELIMITER //
CREATE FUNCTION `last_unassociated_war_log_id` (g_name VARCHAR(30)) RETURNS INT
    BEGIN
        RETURN (SELECT `id` FROM `war_log` WHERE `guild_name` = g_name AND (SELECT `territory_log_id` IS NULL FROM `guild_war_log` WHERE `war_log_id` = `war_log`.`id`) = 1 ORDER BY `created_at` DESC LIMIT 1);
    END; //
DELIMITER ;

# if war log exists within 3 min... associate with the correct `war_log_id` and record of `guild_war_log` table
DELIMITER //
CREATE TRIGGER IF NOT EXISTS `guild_territory_logger`
    AFTER INSERT ON `territory_log` FOR EACH ROW
    BEGIN
        # for old owner guild
        INSERT INTO `guild_war_log` (guild_name, territory_log_id) VALUES (NEW.old_guild_name, NEW.id);

        # for new owner guild
        SET @war_log_id = last_unassociated_war_log_id(NEW.new_guild_name);

        IF @war_log_id IS NOT NULL THEN
            # if the last war log for that guild is within 3 minutes
            IF ((UNIX_TIMESTAMP(NEW.acquired) - UNIX_TIMESTAMP((SELECT `last_up` FROM `war_log` WHERE `id` = @war_log_id)))) <= 180 THEN
                UPDATE `guild_war_log` SET `territory_log_id` = NEW.id WHERE guild_name = NEW.new_guild_name AND `war_log_id` = @war_log_id;
                UPDATE `war_log` SET `ended` = 1 WHERE `id` = @war_log_id;
            ELSE
                INSERT INTO `guild_war_log` (guild_name, territory_log_id) VALUES (NEW.new_guild_name, NEW.id);
            END IF;
        ELSE
            INSERT INTO `guild_war_log` (guild_name, territory_log_id) VALUES (NEW.new_guild_name, NEW.id);
        END IF;
    END; //
DELIMITER ;

DELIMITER //
CREATE TRIGGER IF NOT EXISTS `guild_war_logger`
    AFTER INSERT ON `war_log` FOR EACH ROW
    BEGIN
        IF NEW.guild_name IS NOT NULL THEN
            INSERT INTO `guild_war_log` (guild_name, war_log_id) VALUES (NEW.guild_name, NEW.id);
        END IF;
    END; //
DELIMITER ;

DELIMITER //
CREATE TRIGGER IF NOT EXISTS `guild_war_logger_2`
    AFTER UPDATE ON `war_log` FOR EACH ROW
    BEGIN
        IF NEW.guild_name IS NOT NULL AND OLD.guild_name IS NULL THEN
            INSERT INTO `guild_war_log` (guild_name, war_log_id) VALUES (NEW.guild_name, NEW.id);
        END IF;
    END; //
DELIMITER ;

DROP PROCEDURE IF EXISTS `update_player_war_leaderboard`;
DELIMITER //
CREATE PROCEDURE `update_player_war_leaderboard` (player_uuid CHAR(36), player_name VARCHAR(30))
    BEGIN
        SET @lb = (SELECT `uuid` FROM `player_war_leaderboard` WHERE `uuid` = player_uuid);

        IF @lb IS NULL THEN
            INSERT INTO `player_war_leaderboard` (uuid, last_name, total_war, success_war, survived_war) VALUES
            (
                player_uuid,
                player_name,
                (SELECT COUNT(*) FROM `war_player` p WHERE p.`player_uuid` = player_uuid),
                (SELECT COUNT(*) FROM `war_player` p
                    JOIN `guild_war_log` g ON p.`player_uuid` = player_uuid
                    AND p.war_log_id = g.war_log_id AND g.territory_log_id IS NOT NULL),
                (SELECT COUNT(*) FROM `war_player` p
                    JOIN `guild_war_log` g ON p.`player_uuid` = player_uuid AND NOT p.exited
                    AND p.war_log_id = g.war_log_id AND g.territory_log_id IS NOT NULL)
            );
        ELSE
            UPDATE `player_war_leaderboard`
            SET last_name = player_name,
                total_war = (SELECT COUNT(*) FROM `war_player` g WHERE g.`player_uuid` = player_uuid),
                success_war = (SELECT COUNT(*) FROM `war_player` p
                    JOIN `guild_war_log` g ON p.`player_uuid` = player_uuid
                    AND p.war_log_id = g.war_log_id AND g.territory_log_id IS NOT NULL),
                survived_war = (SELECT COUNT(*) FROM `war_player` p
                    JOIN `guild_war_log` g ON p.`player_uuid` = player_uuid AND NOT p.exited
                    AND p.war_log_id = g.war_log_id AND g.territory_log_id IS NOT NULL)
            WHERE uuid = player_uuid;
        END IF;
    END; //
DELIMITER ;

DELIMITER //
CREATE TRIGGER IF NOT EXISTS `player_war_leaderboard_updater_1`
    AFTER INSERT ON `war_player` FOR EACH ROW
    BEGIN
        IF NEW.player_uuid IS NOT NULL THEN
            CALL update_player_war_leaderboard(NEW.player_uuid, NEW.player_name);
        END IF;
    END; //
DELIMITER ;

DELIMITER //
CREATE TRIGGER IF NOT EXISTS `player_war_leaderboard_updater_2`
    AFTER UPDATE ON `war_player` FOR EACH ROW
    BEGIN
        IF NEW.player_uuid IS NOT NULL THEN
            CALL update_player_war_leaderboard(NEW.player_uuid, NEW.player_name);
        END IF;
    END; //
DELIMITER ;

# User defined timezones for guild / channel / user
CREATE TABLE IF NOT EXISTS `timezone` (
    `discord_id` BIGINT PRIMARY KEY NOT NULL,
    `timezone` VARCHAR(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

# User defined prefixes for guild / channel / user
CREATE TABLE IF NOT EXISTS `prefix` (
    `discord_id` BIGINT PRIMARY KEY NOT NULL,
    `prefix` VARCHAR(10) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

# User defined date formats for guild / channel / user
CREATE TABLE IF NOT EXISTS `date_format` (
    `discord_id` BIGINT PRIMARY KEY NOT NULL,
    `date_format` VARCHAR(30) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

# Ignored channels that the bot should not respond to,
# unless the message itself was ignore command to un-ignore.
CREATE TABLE IF NOT EXISTS `ignore_channel` (
    `channel_id` BIGINT PRIMARY KEY NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
