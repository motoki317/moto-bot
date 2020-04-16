package db.repository.mariadb;

import db.ConnectionPool;
import db.model.territoryList.TerritoryListEntry;
import db.model.territoryList.TerritoryListEntryId;
import db.repository.base.TerritoryListRepository;
import log.Logger;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MariaTerritoryListRepository extends TerritoryListRepository {
    MariaTerritoryListRepository(ConnectionPool db, Logger logger) {
        super(db, logger);
    }

    @Override
    protected TerritoryListEntry bind(@NotNull ResultSet res) throws SQLException {
        return new TerritoryListEntry(res.getLong(1), res.getString(2), res.getString(3));
    }

    @Override
    public <S extends TerritoryListEntry> boolean create(@NotNull S entity) {
        return this.execute(
                "INSERT INTO `territory_list` (user_id, list_name, territory_name) VALUES (?, ?, ?)",
                entity.getUserId(),
                entity.getListName(),
                entity.getTerritoryName()
        );
    }

    @Override
    public boolean exists(@NotNull TerritoryListEntryId territoryListEntryId) {
        ResultSet res = this.executeQuery(
                "SELECT COUNT(*) FROM `territory_list` WHERE `user_id` = ? AND `list_name` = ? AND `territory_name` = ?",
                territoryListEntryId.getUserId(),
                territoryListEntryId.getListName(),
                territoryListEntryId.getTerritoryName()
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
                "SELECT COUNT(*) FROM `territory_list`"
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
    public TerritoryListEntry findOne(@NotNull TerritoryListEntryId territoryListEntryId) {
        ResultSet res = this.executeQuery(
                "SELECT * FROM `territory_list` WHERE `user_id` = ? AND `list_name` = ? AND `territory_name` = ?",
                territoryListEntryId.getUserId(),
                territoryListEntryId.getListName(),
                territoryListEntryId.getTerritoryName()
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
    public Map<String, Integer> getUserLists(long userId) {
        ResultSet res = this.executeQuery(
                "SELECT `list_name`, COUNT(*) FROM `territory_list` WHERE `user_id` = ? GROUP BY `list_name`",
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
    public List<TerritoryListEntry> getList(long userId, @NotNull String listName) {
        ResultSet res = this.executeQuery(
                "SELECT * FROM `territory_list` WHERE `user_id` = ? AND `list_name` = ?",
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
    public List<TerritoryListEntry> findAll() {
        ResultSet res = this.executeQuery(
                "SELECT * FROM `territory_list`"
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
    public boolean update(@NotNull TerritoryListEntry entity) {
        // all fields are in primary key
        return this.create(entity);
    }

    @Override
    public boolean delete(@NotNull TerritoryListEntryId territoryListEntryId) {
        return this.execute(
                "DELETE FROM `territory_list` WHERE `user_id` = ? AND `list_name` = ? AND `territory_name` = ?",
                territoryListEntryId.getUserId(),
                territoryListEntryId.getListName(),
                territoryListEntryId.getTerritoryName()
        );
    }
}
