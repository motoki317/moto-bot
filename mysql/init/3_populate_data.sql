USE `moto-bot`;

DROP PROCEDURE IF EXISTS `populate_player_war_leaderboard`;
DELIMITER //
CREATE PROCEDURE `populate_player_war_leaderboard`()
    BEGIN
        DECLARE cursor_ID CHAR(36);
        DECLARE cursor_NAME VARCHAR(30);
        DECLARE done INT DEFAULT FALSE;
        DECLARE cursor_i CURSOR FOR SELECT DISTINCT `player_uuid` FROM `war_player` WHERE `player_uuid` IS NOT NULL;
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
        OPEN cursor_i;
        read_loop: LOOP
            FETCH cursor_i INTO cursor_ID;
            SELECT `player_name` INTO cursor_NAME FROM `war_player` WHERE `player_uuid` = cursor_ID ORDER BY `war_log_id` DESC LIMIT 1;
            IF done THEN
                LEAVE read_loop;
            END IF;
            CALL update_player_war_leaderboard(cursor_ID, cursor_NAME);
        END LOOP;
        CLOSE cursor_i;
    END; //
DELIMITER ;

CALL populate_player_war_leaderboard();
