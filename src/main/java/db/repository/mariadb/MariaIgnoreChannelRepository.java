package db.repository.mariadb;

import db.ConnectionPool;
import db.model.ignoreChannel.IgnoreChannel;
import db.model.ignoreChannel.IgnoreChannelId;
import db.repository.base.IgnoreChannelRepository;
import log.Logger;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

class MariaIgnoreChannelRepository extends IgnoreChannelRepository {
    MariaIgnoreChannelRepository(ConnectionPool db, Logger logger) {
        super(db, logger);
    }

    @Override
    protected IgnoreChannel bind(@NotNull ResultSet res) throws SQLException {
        return new IgnoreChannel(res.getLong(1));
    }

    @Override
    public <S extends IgnoreChannel> boolean create(@NotNull S entity) {
        return this.execute(
                "INSERT INTO `ignore_channel` (`channel_id`) VALUES (?)",
                entity.getChannelId()
        );
    }

    @Override
    public boolean exists(@NotNull IgnoreChannelId ignoreChannelId) {
        ResultSet res = this.executeQuery(
                "SELECT COUNT(*) FROM `ignore_channel` WHERE `channel_id` = ?",
                ignoreChannelId.getChannelId()
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
                "SELECT COUNT(*) FROM `ignore_channel`"
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
    public IgnoreChannel findOne(@NotNull IgnoreChannelId ignoreChannelId) {
        ResultSet res = this.executeQuery(
                "SELECT * FROM `ignore_channel` WHERE `channel_id` = ?"
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
    public List<IgnoreChannel> findAll() {
        ResultSet res = this.executeQuery(
                "SELECT * FROM `ignore_channel`"
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
    public boolean update(@NotNull IgnoreChannel entity) {
        // this query does not make sense but whatever
        return this.execute(
                "UPDATE `ignore_channel` SET `channel_id` = ? WHERE `channel_id` = ?",
                entity.getChannelId(),
                entity.getChannelId()
        );
    }

    @Override
    public boolean delete(@NotNull IgnoreChannelId ignoreChannelId) {
        return this.execute(
                "DELETE FROM `ignore_channel` WHERE `channel_id` = ?",
                ignoreChannelId.getChannelId()
        );
    }
}
