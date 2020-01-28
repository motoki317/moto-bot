package db.repository;

import db.ConnectionPool;
import db.model.territory.Territory;
import db.model.territory.TerritoryId;
import db.repository.base.Repository;
import log.Logger;
import org.jetbrains.annotations.NotNull;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

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

    /**
     * Counts number of guild territories a guild possesses.
     * @param guildName Guild name.
     * @return Territory count. -1 if something
     */
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

    public static class TerritoryRank {
        private String guildName;
        private int count;
        private int rank;

        private TerritoryRank(String guildName, int count, int rank) {
            this.guildName = guildName;
            this.count = count;
            this.rank = rank;
        }

        public String getGuildName() {
            return guildName;
        }

        public int getCount() {
            return count;
        }

        public int getRank() {
            return rank;
        }
    }

    /**
     * Get ranking of guilds by number of territories.
     * @return Ranking map, where keys are guild names and values are number of territories.
     */
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

    /**
     * Get ranking of guild by number of territories.
     * @param guildName Guild name.
     * @return Ranking. -1 if something went wrong. 0 if the guild does not exist in the ranking.
     */
    public int getGuildTerritoryRankingSpecific(@NotNull String guildName) {
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

    /**
     * Retrieves the latest `acquired` time.
     * @return Latest territory acquired time.
     */
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
     * Updates the whole table to the new given territories list.
     * @param territories New territories list retrieved from the Wynn API.
     * @return True if succeeded.
     */
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
            for (Territory t : newTerritories.values()) {
                if (!oldTerritories.containsKey(t.getName())) {
                    Territory.Location location = t.getLocation();
                    this.logger.debug("Adding new territory: " + t.getName());
                    added++;
                    boolean res = this.execute(connection,
                            "INSERT INTO `territory` (name, guild_name, acquired, attacker, start_x, start_z, end_x, end_z) VALUES " +
                                    "(?, ?, ?, ?, ?, ?, ?, ?)",
                            t.getName(),
                            t.getGuild(),
                            dbFormat.format(t.getAcquired()),
                            t.getAttacker(),
                            location.getStartX(),
                            location.getStartZ(),
                            location.getEndX(),
                            location.getEndZ()
                    );
                    if (!res) throw new SQLException("Insert failed");
                } else {
                    Territory.Location location = t.getLocation();
                    boolean res = this.execute(connection,
                            "UPDATE `territory` SET `guild_name` = ?, `acquired` = ?, `attacker` = ?, `start_x` = ?, `start_z` = ?, `end_x` = ?, `end_z` = ? WHERE `name` = ?",
                            t.getGuild(),
                            dbFormat.format(t.getAcquired()),
                            t.getAttacker(),
                            location.getStartX(),
                            location.getStartZ(),
                            location.getEndX(),
                            location.getEndZ(),
                            t.getName()
                    );
                    if (!res) throw new SQLException("Update failed");
                }
            }
            for (Territory t : oldTerritories.values()) {
                if (!newTerritories.containsKey(t.getName())) {
                    this.logger.debug("Removing territory: " + t.getName());
                    removed++;
                    boolean res = this.execute(connection,
                            "DELETE FROM `territory` WHERE `name` = ?",
                            t.getName()
                    );
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

    @Override
    public boolean delete(@NotNull TerritoryId territoryId) {
        return this.execute(
                "DELETE FROM `territory` WHERE `name` = ?",
                territoryId.getName()
        );
    }
}
