package db.repository.mariadb;

import db.ConnectionPool;
import db.model.timezone.CustomTimeZone;
import db.model.timezone.CustomTimeZoneId;
import db.repository.base.TimeZoneRepository;
import log.Logger;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

class MariaTimeZoneRepository extends MariaRepository<CustomTimeZone> implements TimeZoneRepository {
    MariaTimeZoneRepository(ConnectionPool db, Logger logger) {
        super(db, logger);
    }

    @Override
    protected CustomTimeZone bind(@NotNull ResultSet res) throws SQLException {
        return new CustomTimeZone(res.getLong(1), res.getString(2));
    }

    @Override
    public <S extends CustomTimeZone> boolean create(@NotNull S entity) {
        return this.execute(
                "INSERT INTO `timezone` (discord_id, timezone) VALUES (?, ?)",
                entity.getDiscordId(),
                entity.getTimezone()
        );
    }

    @Override
    public boolean exists(@NotNull CustomTimeZoneId customTimeZoneId) {
        ResultSet res = this.executeQuery(
                "SELECT COUNT(*) FROM `timezone` WHERE `discord_id` = ?",
                customTimeZoneId.getDiscordId()
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
                "SELECT COUNT(*) FROM `timezone`"
        );

        if (res == null) {
            return -1;
        }

        try {
            if (res.next())
                return res.getLong(1);
        } catch (SQLException e) {
            this.logResponseException(e);
        }
        return -1;
    }

    @Nullable
    @Override
    public CustomTimeZone findOne(@NotNull CustomTimeZoneId customTimeZoneId) {
        ResultSet res = this.executeQuery(
                "SELECT * FROM `timezone` WHERE `discord_id` = ?",
                customTimeZoneId.getDiscordId()
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

    @NotNull
    public CustomTimeZone getTimeZone(long... ids) {
        CustomTimeZone ret = CustomTimeZone.getDefault();
        for (long id : ids) {
            CustomTimeZone customTimeZone = this.findOne(() -> id);
            if (customTimeZone != null) {
                ret = customTimeZone;
            }
        }
        return ret;
    }

    @Nullable
    @Override
    public List<CustomTimeZone> findAll() {
        ResultSet res = this.executeQuery(
                "SELECT * FROM `timezone`"
        );

        if (res == null) {
            return null;
        }

        try {
            return bindAll(res);
        } catch (SQLException e) {
            this.logResponseException(e);
        }
        return null;
    }

    @Override
    public boolean update(@NotNull CustomTimeZone entity) {
        return this.execute(
                "UPDATE `timezone` SET `timezone` = ? WHERE `discord_id` = ?",
                entity.getTimezone(),
                entity.getDiscordId()
        );
    }

    @Override
    public boolean delete(@NotNull CustomTimeZoneId customTimeZoneId) {
        return this.execute(
                "DELETE FROM `timezone` WHERE `discord_id` = ?",
                customTimeZoneId.getDiscordId()
        );
    }
}
