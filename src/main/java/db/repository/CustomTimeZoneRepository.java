package db.repository;

import db.ConnectionPool;
import db.model.timezone.CustomTimeZone;
import db.model.timezone.CustomTimeZoneId;
import db.repository.base.Repository;
import log.Logger;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class CustomTimeZoneRepository extends Repository<CustomTimeZone, CustomTimeZoneId> {
    public CustomTimeZoneRepository(ConnectionPool db, Logger logger) {
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

    /**
     * Retrieves custom timezone.
     * If more than one channels are set, channel is the most prioritized, then guild.
     * @param guildId Guild id.
     * @param channelId Channel id.
     * @return Custom timezone. If no custom timezone is set, returns default.
     */
    @NotNull
    public CustomTimeZone getTimeZone(long guildId, long channelId) {
        CustomTimeZone ret = CustomTimeZone.getDefault();
        if (this.exists(() -> guildId)) {
            CustomTimeZone customTimeZone = this.findOne(() -> guildId);
            if (customTimeZone != null) {
                ret = customTimeZone;
            }
        }
        if (this.exists(() -> channelId)) {
            CustomTimeZone customTimeZone = this.findOne(() -> channelId);
            if (customTimeZone != null) {
                ret = customTimeZone;
            }
        }
        return ret;
    }

    /**
     * Retrieves custom timezone.
     * @param userId User id.
     * @return Custom timezone. If no custom timezone is set, returns default.
     */
    @NotNull
    public CustomTimeZone getTimeZone(long userId) {
        CustomTimeZone ret = CustomTimeZone.getDefault();
        if (this.exists(() -> userId)) {
            CustomTimeZone customTimeZone = this.findOne(() -> userId);
            if (customTimeZone != null) {
                ret = customTimeZone;
            }
        }
        return ret;
    }

    /**
     * Retrieves custom timezone.
     * If more than one channels are set, user is the most prioritized, then channel and then guild.
     * @param guildId Guild id.
     * @param channelId Channel id.
     * @param userId User id.
     * @return Custom timezone. If no custom timezone is set, returns default.
     */
    @NotNull
    public CustomTimeZone getTimeZone(long guildId, long channelId, long userId) {
        CustomTimeZone ret = this.getTimeZone(guildId, channelId);
        if (this.exists(() -> userId)) {
            CustomTimeZone customTimeZone = this.findOne(() -> userId);
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
