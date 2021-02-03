package db.repository.mariadb;

import db.ConnectionPool;
import db.model.guildList.GuildListEntry;
import db.model.guildList.GuildListEntryId;
import db.repository.base.GuildListRepository;
import log.Logger;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MariaGuildListRepository extends MariaRepository<GuildListEntry> implements GuildListRepository {
    MariaGuildListRepository(ConnectionPool db, Logger logger) {
        super(db, logger);
    }

    @Override
    protected GuildListEntry bind(@NotNull ResultSet res) throws SQLException {
        return new GuildListEntry(res.getLong(1), res.getString(2), res.getString(3));
    }

    @Override
    public <S extends GuildListEntry> boolean create(@NotNull S entity) {
        return this.execute(
                "INSERT INTO `guild_list` (user_id, list_name, guild_name) VALUES (?, ?, ?)",
                entity.getUserId(),
                entity.getListName(),
                entity.getGuildName()
        );
    }

    @Override
    public boolean exists(@NotNull GuildListEntryId guildListEntryId) {
        ResultSet res = this.executeQuery(
                "SELECT COUNT(*) FROM `guild_list` WHERE `user_id` = ? AND `list_name` = ? AND `guild_name` = ?",
                guildListEntryId.getUserId(),
                guildListEntryId.getListName(),
                guildListEntryId.getGuildName()
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
                "SELECT COUNT(*) FROM `guild_list`"
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
    public GuildListEntry findOne(@NotNull GuildListEntryId guildListEntryId) {
        ResultSet res = this.executeQuery(
                "SELECT COUNT(*) FROM `guild_list` WHERE `user_id` = ? AND `list_name` = ? AND `guild_name` = ?",
                guildListEntryId.getUserId(),
                guildListEntryId.getListName(),
                guildListEntryId.getGuildName()
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
    public @Nullable Map<String, Integer> getUserLists(long userId) {
        ResultSet res = this.executeQuery(
                "SELECT `list_name`, COUNT(*) FROM `guild_list` WHERE `user_id` = ? GROUP BY `list_name`",
                userId
        );

        if (res == null) {
            return null;
        }

        try {
            Map<String, Integer> ret = new HashMap<>();
            while (res.next()) {
                ret.put(res.getString(1), res.getInt(2));
            }
            return ret;
        } catch (SQLException e) {
            this.logResponseException(e);
            return null;
        }
    }

    @Override
    public List<GuildListEntry> getList(long userId, @NotNull String listName) {
        ResultSet res = this.executeQuery(
                "SELECT * FROM `guild_list` WHERE `user_id` = ? AND `list_name` = ?",
                userId, listName
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
    public List<GuildListEntry> findAll() {
        ResultSet res = this.executeQuery(
                "SELECT COUNT(*) FROM `guild_list`"
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
    public boolean update(@NotNull GuildListEntry entity) {
        // all columns are in primary key
        return this.create(entity);
    }

    @Override
    public boolean delete(@NotNull GuildListEntryId guildListEntryId) {
        return this.execute(
                "DELETE FROM `guild_list` WHERE `user_id` = ? AND `list_name` = ? AND `guild_name` = ?",
                guildListEntryId.getUserId(),
                guildListEntryId.getListName(),
                guildListEntryId.getGuildName()
        );
    }
}
