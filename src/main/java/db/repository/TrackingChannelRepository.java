package db.repository;

import db.model.TrackingChannel;
import log.Logger;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TrackingChannelRepository extends Repository<TrackingChannel> {
    public TrackingChannelRepository(Connection db, Logger logger) {
        super(db, logger);
    }

    @Override
    public void create(TrackingChannel entity) {
        this.execute(
                "INSERT INTO `tracking_channels` (`guild_id`, `channel_id`, `type`) VALUES (?, ?, ?)",
                entity.getGuildId(),
                entity.getChannelId(),
                entity.getType()
        );
    }

    @Override
    public boolean exists(TrackingChannel entity) {
        ResultSet res = this.executeQuery(
                "SELECT COUNT(*) FROM `tracking_channels` WHERE `guild_id` = ? AND `channel_id` = ? AND `type` = ?",
                entity.getGuildId(),
                entity.getChannelId(),
                entity.getType()
        );
        if (res == null) return false;

        try {
            return res.getInt(1) > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public long count() {
        return 0;
    }

    @Override
    public TrackingChannel findOne(TrackingChannel entity) {
        return null;
    }

    @Override
    public Iterable<TrackingChannel> findAll() {
        return null;
    }

    @Override
    public void update(TrackingChannel entity) {

    }

    @Override
    public void delete(TrackingChannel entity) {

    }
}
