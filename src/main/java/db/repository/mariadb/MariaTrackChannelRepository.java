package db.repository.mariadb;

import db.ConnectionPool;
import db.model.track.TrackChannel;
import db.model.track.TrackChannelId;
import db.model.track.TrackType;
import db.repository.base.TrackChannelRepository;
import log.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

class MariaTrackChannelRepository extends TrackChannelRepository {
    private static final DateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    MariaTrackChannelRepository(ConnectionPool db, Logger logger) {
        super(db, logger);
    }

    protected TrackChannel bind(@NotNull ResultSet res) throws SQLException {
        TrackChannel instance = new TrackChannel(
                TrackType.valueOf(res.getString(1)), res.getLong(2), res.getLong(3),
                res.getLong(6), res.getTimestamp(7)
        );
        instance.setGuildName(res.getString(4));
        instance.setPlayerUUID(res.getString(5));
        return instance;
    }

    @Override
    public boolean create(@NotNull TrackChannel entity) {
        return this.execute(
                "INSERT INTO `track_channel` (`type`, `guild_id`, `channel_id`, `guild_name`, `player_uuid`, `user_id`, `expires_at`) VALUES (?, ?, ?, ?, ?, ?, ?)",
                entity.getType(),
                entity.getGuildId(),
                entity.getChannelId(),
                entity.getGuildName(),
                entity.getPlayerUUID(),
                entity.getUserId(),
                dbFormat.format(entity.getExpiresAt())
        );
    }

    @Override
    public boolean exists(@NotNull TrackChannelId id) {
        ResultSet res = this.executeQuery(
                "SELECT COUNT(*) FROM `track_channel` WHERE `type` = ? AND `guild_id` = ? AND `channel_id` = ? AND `guild_name` <=> ? AND `player_uuid` <=> ?",
                id.getType(),
                id.getGuildId(),
                id.getChannelId(),
                id.getGuildName(),
                id.getPlayerUUID()
        );
        if (res == null) return false;

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
        ResultSet res = this.executeQuery("SELECT COUNT(*) FROM `track_channel`");
        if (res == null) return 0;

        try {
            if (res.next())
                return res.getInt(1);
        } catch (SQLException e) {
            this.logResponseException(e);
        }
        return 0;
    }

    @Override
    public TrackChannel findOne(@NotNull TrackChannelId id) {
        ResultSet res = this.executeQuery(
                "SELECT * FROM `track_channel` WHERE `type` = ? AND `guild_id` = ? AND `channel_id` = ? AND `guild_name` <=> ? AND `player_uuid` <=> ? LIMIT 1",
                id.getType(),
                id.getGuildId(),
                id.getChannelId(),
                id.getGuildName(),
                id.getPlayerUUID()
        );
        if (res == null) return null;

        try {
            if (res.next())
                return bind(res);
        } catch (SQLException e) {
            this.logResponseException(e);
        }
        return null;
    }

    @Nullable
    public List<TrackChannel> findAllOf(long guildId, long channelId) {
        ResultSet res = this.executeQuery(
                "SELECT * FROM `track_channel` WHERE `guild_id` = ? AND `channel_id` = ?",
                guildId,
                channelId
        );
        if (res == null) return null;

        try {
            return bindAll(res);
        } catch (SQLException e) {
            this.logResponseException(e);
            return null;
        }
    }

    @Override
    public @Nullable List<TrackChannel> findAllOfGuildNameAndType(String guildName, TrackType type) {
        ResultSet res = this.executeQuery(
                "SELECT * FROM `track_channel` WHERE `guild_name` = ? AND `type` = ?",
                guildName,
                type
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
    public @Nullable List<TrackChannel> findAllOfPlayerUUIDAndType(String playerUUID, TrackType type) {
        ResultSet res = this.executeQuery(
                "SELECT * FROM `track_channel` WHERE `player_uuid` = ? AND `type` = ?",
                playerUUID,
                type
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

    @Nullable
    @Override
    public List<TrackChannel> findAll() {
        ResultSet res = this.executeQuery("SELECT * FROM `track_channel`");

        if (res == null) return null;

        try {
            return bindAll(res);
        } catch (SQLException e) {
            this.logResponseException(e);
        }
        return null;
    }

    @Override
    public boolean update(@NotNull TrackChannel entity) {
        return this.execute(
                "UPDATE `track_channel` SET `user_id` = ?, `expires_at` = ? WHERE `type` = ? AND `guild_id` = ? AND `channel_id` = ? AND `guild_name` <=> ? AND `player_uuid` <=> ?",
                entity.getUserId(),
                dbFormat.format(entity.getExpiresAt()),
                entity.getType(),
                entity.getGuildId(),
                entity.getChannelId(),
                entity.getGuildName(),
                entity.getPlayerUUID()
        );
    }

    @Override
    public boolean delete(@NotNull TrackChannelId id) {
        return this.execute(
                "DELETE FROM `track_channel` WHERE `type` = ? AND `guild_id` = ? AND `channel_id` = ? AND `guild_name` <=> ? AND `player_uuid` <=> ?",
                id.getType(),
                id.getGuildId(),
                id.getChannelId(),
                id.getGuildName(),
                id.getPlayerUUID()
        );
    }

    @Nullable
    public List<TrackChannel> findAllOfType(TrackType type) {
        ResultSet res = this.executeQuery(
                "SELECT * FROM `track_channel` WHERE `type` = ?",
                type
        );

        if (res == null) return null;

        try {
            return bindAll(res);
        } catch (SQLException e) {
            this.logResponseException(e);
            return null;
        }
    }
}
