package db.repository.mariadb;

import db.ConnectionPool;
import db.model.territoryLog.TerritoryActivity;
import db.model.territoryLog.TerritoryLog;
import db.model.territoryLog.TerritoryLogId;
import db.repository.base.TerritoryLogRepository;
import log.Logger;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class MariaTerritoryLogRepository extends TerritoryLogRepository {
    private static final DateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    MariaTerritoryLogRepository(ConnectionPool db, Logger logger) {
        super(db, logger);
    }

    @Override
    protected TerritoryLog bind(@NotNull ResultSet res) throws SQLException {
        return new TerritoryLog(res.getInt(1), res.getString(2), res.getString(3), res.getString(4),
                res.getInt(5), res.getInt(6), res.getTimestamp(7), res.getLong(8));
    }

    private TerritoryActivity bindActivity(@NotNull ResultSet res) throws SQLException {
        return new TerritoryActivity(res.getString(1), res.getInt(2));
    }

    private List<TerritoryActivity> bindAllActivities(@NotNull ResultSet res) throws SQLException {
        List<TerritoryActivity> ret = new ArrayList<>();
        while (res.next()) {
            ret.add(bindActivity(res));
        }
        return ret;
    }

    @Override
    public <S extends TerritoryLog> boolean create(@NotNull S entity) {
        throw new Error("Insert not implemented: records are automatically created by the triggers");
    }

    @Override
    public boolean exists(@NotNull TerritoryLogId territoryLogId) {
        ResultSet res = this.executeQuery(
                "SELECT COUNT(*) FROM `territory_log` WHERE `id` = ?",
                territoryLogId.getId()
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
                "SELECT COUNT(*) FROM `territory_log`"
        );

        if (res == null) {
            return 0;
        }

        try {
            if (res.next())
                return res.getInt(1);
        } catch (SQLException e) {
            this.logResponseException(e);
        }
        return 0;
    }

    public int lastInsertId() {
        ResultSet res = this.executeQuery(
                "SELECT MAX(`id`) FROM `territory_log`"
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
    public List<TerritoryLog> findAllInRange(int oldId, int newId) {
        ResultSet res = this.executeQuery(
                "SELECT * FROM `territory_log` WHERE `id` > ? AND `id` <= ?",
                oldId,
                newId
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
    public TerritoryLog findOne(@NotNull TerritoryLogId territoryLogId) {
        ResultSet res = this.executeQuery(
                "SELECT * FROM `territory_log` WHERE `id` = ?",
                territoryLogId.getId()
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
    public List<TerritoryLog> findAllIn(List<Integer> ids) {
        if (ids.isEmpty()) {
            return new ArrayList<>();
        }

        String placeHolder = String.format("(%s)",
                ids.stream().map(i -> "?").collect(Collectors.joining(", "))
        );
        ResultSet res = this.executeQuery(
                "SELECT * FROM `territory_log` WHERE `id` IN " + placeHolder,
                ids.toArray()
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
    public int territoryLogCount(String territoryName) {
        ResultSet res = this.executeQuery(
                "SELECT COUNT(*) FROM `territory_log` WHERE `territory_name` = ?",
                territoryName
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
    public List<TerritoryLog> territoryLogs(String territoryName, int limit, int offset) {
        ResultSet res = this.executeQuery(
                "SELECT * FROM `territory_log` WHERE `territory_name` = ?" +
                        " ORDER BY `id` DESC LIMIT " + limit + " OFFSET " + offset,
                territoryName
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
    public List<TerritoryActivity> territoryActivity() {
        ResultSet res = this.executeQuery(
                "SELECT `territory_name`, COUNT(*) FROM `territory_log` GROUP BY `territory_name`"
        );

        if (res == null) {
            return null;
        }

        try {
            return bindAllActivities(res);
        } catch (SQLException e) {
            this.logResponseException(e);
            return null;
        }
    }

    @Override
    public List<TerritoryActivity> territoryActivity(@NotNull Date start, @NotNull Date end) {
        ResultSet res = this.executeQuery(
                "SELECT `territory_name`, COUNT(*) FROM `territory_log`" +
                        " WHERE `acquired` >= ? AND `acquired` < ? GROUP BY `territory_name`",
                dbFormat.format(start),
                dbFormat.format(end)
        );

        if (res == null) {
            return null;
        }

        try {
            return bindAllActivities(res);
        } catch (SQLException e) {
            this.logResponseException(e);
            return null;
        }
    }

    @Nullable
    @Override
    public List<TerritoryLog> findAll() {
        throw new Error("FindAll not implemented: record counts may be too large, use limit / offset queries instead");
    }

    @Override
    public boolean update(@NotNull TerritoryLog entity) {
        throw new Error("Update not implemented: records are automatically created by the triggers");
    }

    @Override
    public boolean delete(@NotNull TerritoryLogId territoryLogId) {
        throw new Error("Delete not implemented: records are automatically created by the triggers");
    }
}
