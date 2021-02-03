package db.repository.mariadb;

import db.ConnectionPool;
import db.model.playerNumber.PlayerNumber;
import db.model.playerNumber.PlayerNumberId;
import db.repository.base.PlayerNumberRepository;
import log.Logger;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

class MariaPlayerNumberRepository extends MariaRepository<PlayerNumber> implements PlayerNumberRepository {
    private static final DateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    MariaPlayerNumberRepository(ConnectionPool db, Logger logger) {
        super(db, logger);
    }

    @Override
    protected PlayerNumber bind(@NotNull ResultSet res) throws SQLException {
        return new PlayerNumber(res.getTimestamp(1), res.getInt(2));
    }

    @Override
    public <S extends PlayerNumber> boolean create(@NotNull S entity) {
        return this.execute(
                "INSERT INTO `player_number` (date_time, player_num) VALUES (?, ?)",
                dbFormat.format(entity.getDateTime()),
                entity.getPlayerNum()
        );
    }

    @Override
    public boolean exists(@NotNull PlayerNumberId playerNumberId) {
        ResultSet res = this.executeQuery(
                "SELECT COUNT(*) FROM `player_number` WHERE `date_time` = ?",
                dbFormat.format(playerNumberId.getDateTime())
        );

        if (res == null) {
            return false;
        }

        try {
            if (res.next())
                return res.getInt(1) > 0;
        } catch (SQLException e) {
            this.logResponseException(e);
        }
        return false;
    }

    @Override
    public long count() {
        ResultSet res = this.executeQuery(
                "SELECT COUNT(*) FROM `player_number`"
        );

        if (res == null) {
            return -1;
        }

        try {
            if (res.next())
                return res.getInt(1);
        } catch (SQLException e) {
            this.logResponseException(e);
        }
        return -1;
    }

    @Nullable
    @Override
    public PlayerNumber findOne(@NotNull PlayerNumberId playerNumberId) {
        ResultSet res = this.executeQuery(
                "SELECT * FROM `player_number` WHERE `date_time` = ?",
                dbFormat.format(playerNumberId.getDateTime())
        );

        if (res == null) {
            return null;
        }

        try {
            if (res.next())
                return bind(res);
        } catch (SQLException e) {
            this.logResponseException(e);
        }
        return null;
    }

    @Nullable
    @Override
    public List<PlayerNumber> findAll() {
        ResultSet res = this.executeQuery(
                "SELECT * FROM `player_number`"
        );

        if (res == null) {
            return null;
        }

        try {
            return bindAll(res);
        } catch (SQLException e) {
            this.logResponseException(e);
            return null;
        }
    }

    @Override
    public boolean update(@NotNull PlayerNumber entity) {
        return this.execute(
                "UPDATE `player_number` SET `player_num` = ? WHERE `date_time` = ?",
                dbFormat.format(entity.getDateTime())
        );
    }

    @Override
    public boolean delete(@NotNull PlayerNumberId playerNumberId) {
        return this.execute(
                "DELETE FROM `player_number` WHERE `date_time` = ?",
                dbFormat.format(playerNumberId.getDateTime())
        );
    }

    @Override
    public PlayerNumber max() {
        ResultSet res = this.executeQuery(
                "SELECT * FROM `player_number` WHERE `player_num` = (SELECT MAX(`player_num`) FROM `player_number`)"
        );

        if (res == null) {
            return null;
        }

        try {
            if (res.next())
                return bind(res);
        } catch (SQLException e) {
            this.logResponseException(e);
        }
        return null;
    }

    @Override
    public PlayerNumber min() {
        ResultSet res = this.executeQuery(
                "SELECT * FROM `player_number` WHERE `player_num` = (SELECT MIN(`player_num`) FROM `player_number`)"
        );

        if (res == null) {
            return null;
        }

        try {
            if (res.next())
                return bind(res);
        } catch (SQLException e) {
            this.logResponseException(e);
        }
        return null;
    }

    @Override
    public Date oldestDate() {
        ResultSet res = this.executeQuery(
                "SELECT MIN(`date_time`) FROM `player_number`"
        );

        if (res == null) {
            return null;
        }

        try {
            if (res.next())
                return res.getTimestamp(1);
        } catch (SQLException e) {
            this.logResponseException(e);
        }
        return null;
    }

    @Override
    public boolean deleteAll() {
        return this.execute(
                "TRUNCATE TABLE `player_number`"
        );
    }
}
