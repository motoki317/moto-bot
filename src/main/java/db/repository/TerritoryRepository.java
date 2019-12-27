package db.repository;

import db.ConnectionPool;
import db.model.territory.Territory;
import db.model.territory.TerritoryId;
import db.repository.base.Repository;
import log.Logger;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

public class TerritoryRepository extends Repository<Territory, TerritoryId> {
    private static final DateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public TerritoryRepository(ConnectionPool db, Logger logger) {
        super(db, logger);
    }

    @Override
    protected Territory bind(@NotNull ResultSet res) throws SQLException {
        Territory.Location location = new Territory.Location(res.getInt(5), res.getInt(6), res.getInt(7),res.getInt(8));
        return new Territory(res.getString(1), res.getString(2), res.getTimestamp(3), res.getString(4), location);
    }

    @Override
    public <S extends Territory> boolean create(@NotNull S entity) {
        Territory.Location location = entity.getLocation();
        return this.execute(
                "INSERT INTO `territory` (name, guild_name, acquired, attacker, start_x, start_z, end_x, end_z) VALUES " +
                        "(?, ?, ?, ?, ?, ?, ?, ?)",
                entity.getName(),
                entity.getGuild(),
                dbFormat.format(entity.getAcquired()),
                entity.getAttacker(),
                location.getStartX(),
                location.getStartZ(),
                location.getEndX(),
                location.getEndZ()
        );
    }

    @Override
    public boolean exists(@NotNull TerritoryId territoryId) {
        ResultSet res = this.executeQuery(
                "SELECT COUNT(*) FROM `territory` WHERE `name` = ?",
                territoryId.getName()
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
                "SELECT COUNT(*) FROM `territory`"
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

    @Nullable
    @Override
    public Territory findOne(@NotNull TerritoryId territoryId) {
        ResultSet res = this.executeQuery(
                "SELECT * FROM `territory` WHERE `name` = ?",
                territoryId.getName()
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
    public List<Territory> findAll() {
        ResultSet res = this.executeQuery(
                "SELECT * FROM `territory`"
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
    public boolean update(@NotNull Territory entity) {
        Territory.Location location = entity.getLocation();
        return this.execute(
                "UPDATE `territory` SET `guild_name` = ?, `acquired` = ?, `attacker` = ?, `start_x` = ?, `start_z` = ?, `end_x` = ?, `end_z` = ? WHERE `name` = ?",
                entity.getGuild(),
                dbFormat.format(entity.getAcquired()),
                entity.getAttacker(),
                location.getStartX(),
                location.getStartZ(),
                location.getEndX(),
                location.getEndZ(),
                entity.getName()
        );
    }

    @Override
    public boolean delete(@NotNull TerritoryId territoryId) {
        return this.execute(
                "DELETE FROM `territory` WHERE `name` = ?",
                territoryId.getName()
        );
    }
}
