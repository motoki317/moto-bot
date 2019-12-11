package db.repository;

import db.model.TrackingChannel;
import db.model.TrackingType;
import db.repository.base.Repository;
import log.Logger;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TrackingChannelRepository extends Repository<TrackingChannel> {
    public TrackingChannelRepository(Connection db, Logger logger) {
        super(db, logger);
    }

    @Override
    public void create(TrackingChannel entity) {
        this.execute(
                "INSERT INTO `tracking_channel` (`type`, `guild_id`, `channel_id`) VALUES (?, ?, ?)",
                entity.getType(),
                entity.getGuildId(),
                entity.getChannelId()
        );
    }

    @Override
    public boolean exists(TrackingChannel entity) {
        ResultSet res = this.executeQuery(
                "SELECT COUNT(*) FROM `tracking_channel` WHERE `type` = ? AND `guild_id` = ? AND `channel_id` = ?",
                entity.getType(),
                entity.getGuildId(),
                entity.getChannelId()
        );
        if (res == null) return false;

        try {
            if (res.next())
                return res.getInt(1) > 0;
        } catch (SQLException e) {
            this.logResponseError(e);
        }
        return false;
    }

    @Override
    public long count() {
        ResultSet res = this.executeQuery("SELECT COUNT(*) FROM `tracking_channel`");
        if (res == null) return 0;

        try {
            if (res.next())
                return res.getInt(1);
        } catch (SQLException e) {
            this.logResponseError(e);
        }
        return 0;
    }

    @Override
    public TrackingChannel findOne(TrackingChannel entity) {
        if (exists(entity))
            return entity;
        return null;
    }

    @Override
    public List<TrackingChannel> findAll() {
        ResultSet res = this.executeQuery("SELECT type, guild_id, channel_id FROM `tracking_channel`");

        List<TrackingChannel> ret = new ArrayList<>();
        if (res == null)
            return ret;

        try {
            while (res.next()) {
                ret.add(bind(res.getString(1), res.getLong(2), res.getLong(3)));
            }
        } catch (SQLException e) {
            this.logResponseError(e);
        }
        return ret;
    }

    @Override
    public void update(TrackingChannel entity) {
        this.logger.logError("", new Exception("Not implemented"));
    }

    @Override
    public void delete(TrackingChannel entity) {
        this.execute(
                "DELETE FROM tracking_channel WHERE type = ? AND guild_id = ? AND channel_id = ?",
                entity.getType(),
                entity.getGuildId(),
                entity.getChannelId()
        );
    }

    public List<TrackingChannel> findAllOfType(TrackingType type) {
        ResultSet res = this.executeQuery(
                "SELECT guild_id, channel_id FROM tracking_channel WHERE type = ?",
                type
        );

        List<TrackingChannel> ret = new ArrayList<>();
        if (res == null)
            return ret;
        try {
            while (res.next()) {
                ret.add(bind(type.toString(), res.getLong(1), res.getLong(2)));
            }
        } catch (SQLException e) {
            this.logResponseError(e);
        }
        return ret;
    }

    private static TrackingChannel bind(String type, long guildId, long channelId) {
        return new TrackingChannel(TrackingType.valueOf(type), guildId, channelId);
    }
}
