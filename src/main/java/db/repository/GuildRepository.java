package db.repository;

import db.ConnectionPool;
import db.model.guild.Guild;
import db.model.guild.GuildId;
import db.repository.base.Repository;
import log.Logger;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

public class GuildRepository extends Repository<Guild, GuildId> {
    private static final DateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public GuildRepository(ConnectionPool db, Logger logger) {
        super(db, logger);
    }

    @Override
    protected Guild bind(@NotNull ResultSet res) throws SQLException {
        return new Guild(res.getString(1), res.getString(2), res.getTimestamp(3));
    }

    @Override
    public <S extends Guild> boolean create(@NotNull S entity) {
        return this.execute(
                "INSERT INTO `guild` (name, prefix, created_at) VALUES (?, ?, ?)",
                entity.getName(),
                entity.getPrefix(),
                dbFormat.format(entity.getCreatedAt())
        );
    }

    @Override
    public boolean exists(@NotNull GuildId guildId) {
        ResultSet res = this.executeQuery(
                "SELECT COUNT(*) FROM `guild` WHERE `name` = ?",
                guildId.getName()
        );

        if (res == null) {
            return false;
        }

        try {
            if (res.next())
                return res.getInt(1) > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public long count() {
        ResultSet res = this.executeQuery(
                "SELECT COUNT(*) FROM `guild`"
        );

        if (res == null) {
            return -1;
        }

        try {
            if (res.next())
                return res.getLong(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    @Nullable
    @Override
    public Guild findOne(@NotNull GuildId guildId) {
        ResultSet res = this.executeQuery(
                "SELECT * FROM `guild` WHERE `name` = ?",
                guildId.getName()
        );

        if (res == null) {
            return null;
        }

        try {
            if (res.next())
                return bind(res);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Nullable
    @Override
    public List<Guild> findAll() {
        ResultSet res = this.executeQuery(
                "SELECT * FROM `guild`"
        );

        if (res == null) {
            return null;
        }

        try {
            return bindAll(res);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean update(@NotNull Guild entity) {
        return this.execute(
                "UPDATE `guild` SET `prefix` = ?, `created_at` = ? WHERE `name` = ?",
                entity.getPrefix(),
                dbFormat.format(entity.getCreatedAt()),
                entity.getName()
        );
    }

    @Override
    public boolean delete(@NotNull GuildId guildId) {
        return this.execute(
                "DELETE FROM `guild` WHERE `name` = ?",
                guildId.getName()
        );
    }
}