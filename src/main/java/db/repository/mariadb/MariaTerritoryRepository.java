package db.repository.mariadb;

import db.ConnectionPool;
import db.model.territory.Territory;
import db.model.territory.TerritoryId;
import db.model.territory.TerritoryRank;
import db.repository.base.TerritoryRepository;
import log.Logger;
import org.jetbrains.annotations.NotNull;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class MariaTerritoryRepository extends TerritoryRepository {
    private static final DateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    MariaTerritoryRepository(ConnectionPool db, Logger logger) {
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

    public int countGuildTerritories(@NotNull String guildName) {
        ResultSet res = this.executeQuery(
                "SELECT count_guild_territories(?)",
                guildName
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
    public List<Territory> getGuildTerritories(@NotNull String guildName) {
        ResultSet res = this.executeQuery(
                "SELECT * FROM `territory` WHERE `guild_name` = ?",
                guildName
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
    public List<TerritoryRank> getGuildTerritoryNumbers() {
        ResultSet res = this.executeQuery(
                "SELECT `guild_name`, COUNT(*) AS `territories`, RANK() OVER (ORDER BY COUNT(*) DESC) FROM `territory` GROUP BY `guild_name`"
        );

        if (res == null) {
            return null;
        }

        try {
            List<TerritoryRank> ranking = new ArrayList<>();
            while (res.next()) {
                String guildName = res.getString(1);
                int territories = res.getInt(2);
                int rank = res.getInt(3);
                if (guildName == null) {
                    throw new SQLException("Returned row was null");
                }
                ranking.add(new TerritoryRank(guildName, territories, rank));
            }
            return ranking;
        } catch (SQLException e) {
            this.logResponseException(e);
        }
        return null;
    }

    public int getGuildTerritoryRanking(@NotNull String guildName) {
        ResultSet res = this.executeQuery(
                "SELECT `ttn`.`rank` FROM (SELECT `guild_name`, RANK() OVER (ORDER BY COUNT(*) DESC) AS `rank` FROM `territory` GROUP BY `guild_name`) AS ttn WHERE `guild_name` = ?",
                guildName
        );

        if (res == null) {
            return -1;
        }

        try {
            if (res.next()) {
                return res.getInt(1);
            } else {
                return 0;
            }
        } catch (SQLException e) {
            this.logResponseException(e);
            return -1;
        }
    }

    @Nullable
    public Date getLatestAcquiredTime() {
        ResultSet res = this.executeQuery(
                "SELECT MAX(`acquired`) FROM `territory`"
        );

        if (res == null) {
            return null;
        }

        try {
            if (res.next())
                return res.getTimestamp(1);
        } catch (SQLException e) {
            this.logResponseException(e);
        }
        return null;
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

    /**
     * Creates entity using given connection.
     * @param connection SQL connection.
     * @param entity Territory entity.
     * @return {@code true} if success.
     */
    private boolean create(Connection connection, Territory entity) {
        Territory.Location location = entity.getLocation();
        return this.execute(connection,
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

    /**
     * Updates entity using given connection.
     * @param connection SQL connection.
     * @param entity Territory entity.
     * @return {@code true} if success.
     */
    private boolean update(Connection connection, Territory entity) {
        Territory.Location location = entity.getLocation();
        return this.execute(connection,
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

    /**
     * Deletes entity using given connection.
     * @param connection SQL connection.
     * @param entity Territory entity.
     * @return {@code true} if success.
     */
    private boolean delete(Connection connection, TerritoryId entity) {
        return this.execute(connection,
                "DELETE FROM `territory` WHERE `name` = ?",
                entity.getName()
        );
    }

    @CheckReturnValue
    public boolean updateAll(@NotNull List<Territory> territories) {
        Connection connection = this.db.getConnection();
        if (connection == null) {
            return false;
        }

        try {
            connection.setAutoCommit(false);

            Map<String, Territory> newTerritories = territories.stream().collect(Collectors.toMap(Territory::getName, t -> t));
            List<Territory> oldTerritoryList = this.findAll();
            if (oldTerritoryList == null) return false;
            Map<String, Territory> oldTerritories = oldTerritoryList.stream().collect(Collectors.toMap(Territory::getName, t -> t));

            int added = 0;
            int removed = 0;
            for (Territory territory : newTerritories.values()) {
                if (!oldTerritories.containsKey(territory.getName())) {
                    this.logger.debug("Adding new territory: " + territory.getName());
                    added++;
                    boolean res = this.create(connection, territory);
                    if (!res) throw new SQLException("Insert failed");
                } else {
                    boolean res = this.update(connection, territory);
                    if (!res) throw new SQLException("Update failed");
                }
            }
            for (Territory territory : oldTerritories.values()) {
                if (!newTerritories.containsKey(territory.getName())) {
                    this.logger.debug("Removing territory: " + territory.getName());
                    removed++;
                    boolean res = this.delete(connection, territory);
                    if (!res) throw new SQLException("Delete failed");
                }
            }

            connection.commit();

            if (added != 0 || removed != 0) {
                this.logger.log(0, String.format("Added %s, and removed %s territories.", added, removed));
            }

            return true;
        } catch (SQLException e) {
            this.logger.logException("an exception occurred while updating territories.", e);
            this.logger.log(0, "Rolling back territory update changes.");
            try {
                connection.rollback();
            } catch (SQLException ex) {
                this.logger.logException("an exception occurred while rolling back changes.", ex);
            }
            return false;
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                this.logger.logException("an exception occurred while setting back auto commit to on.", e);
            }
            this.db.releaseConnection(connection);
        }
    }

    @Nullable
    @Override
    public List<String> territoryNamesBeginsWith(String prefix) {
        prefix += "%";

        ResultSet res = this.executeQuery(
                "SELECT `name` FROM `territory` WHERE `name` LIKE ?",
                prefix
        );

        if (res == null) {
            return null;
        }

        try {
            List<String> ret = new ArrayList<>();
            while (res.next()) {
                ret.add(res.getString(1));
            }
            return ret;
        } catch (SQLException e) {
            this.logResponseException(e);
        }
        return null;
    }

    @Override
    public boolean delete(@NotNull TerritoryId territoryId) {
        return this.execute(
                "DELETE FROM `territory` WHERE `name` = ?",
                territoryId.getName()
        );
    }
}
