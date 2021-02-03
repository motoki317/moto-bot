package db.repository.mariadb;

import db.ConnectionPool;
import db.model.warTrack.WarTrack;
import db.model.warTrack.WarTrackId;
import db.repository.base.WarTrackRepository;
import log.Logger;
import org.jetbrains.annotations.NotNull;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

class MariaWarTrackRepository extends MariaRepository<WarTrack> implements WarTrackRepository {
    MariaWarTrackRepository(ConnectionPool db, Logger logger) {
        super(db, logger);
    }

    @Override
    protected WarTrack bind(@NotNull ResultSet res) throws SQLException {
        return new WarTrack(res.getInt(1), res.getLong(2), res.getLong(3));
    }

    @Override
    public <S extends WarTrack> boolean create(@NotNull S entity) {
        return this.execute(
                "INSERT INTO `war_track` (war_log_id, discord_channel_id, discord_message_id) VALUES (?, ?, ?)",
                entity.getWarLogId(),
                entity.getChannelId(),
                entity.getMessageId()
        );
    }

    @Override
    public boolean exists(@NotNull WarTrackId warTrackId) {
        ResultSet res = this.executeQuery(
                "SELECT COUNT(*) FROM `war_track` WHERE `war_log_id` = ? AND `discord_channel_id` = ?",
                warTrackId.getWarLogId(),
                warTrackId.getChannelId()
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
                "SELECT COUNT(*) FROM `war_track`"
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
    public WarTrack findOne(@NotNull WarTrackId warTrackId) {
        ResultSet res = this.executeQuery(
                "SELECT * FROM `war_track` WHERE `war_log_id` = ? AND `discord_channel_id` = ?",
                warTrackId.getWarLogId(),
                warTrackId.getChannelId()
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
    public List<WarTrack> findAll() {
        ResultSet res = this.executeQuery(
                "SELECT * FROM `war_track`"
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

    @CheckReturnValue
    @Nullable
    public List<WarTrack> findAllOfWarLogId(int id) {
        ResultSet res = this.executeQuery(
                "SELECT * FROM `war_track` WHERE `war_log_id` = ?",
                id
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
    public boolean update(@NotNull WarTrack entity) {
        return this.execute(
                "UPDATE `war_track` SET `discord_message_id` = ? WHERE `war_log_id` = ? AND `discord_channel_id` = ?",
                entity.getMessageId(),
                entity.getWarLogId(),
                entity.getChannelId()
        );
    }

    @Override
    public boolean delete(@NotNull WarTrackId warTrackId) {
        return this.execute(
                "DELETE FROM `war_track` WHERE `war_log_id` = ? AND `discord_channel_id` = ?",
                warTrackId.getWarLogId(),
                warTrackId.getChannelId()
        );
    }

    @CheckReturnValue
    public boolean deleteAllOfLogEnded() {
        return this.execute(
                "DELETE `war_track` FROM `war_track` JOIN `war_log` ON `war_log`.`id` = `war_track`.`war_log_id` WHERE `war_log`.`log_ended` = 1"
        );
    }
}
