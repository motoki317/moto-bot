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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    @CheckReturnValue
    public boolean updateAll(@NotNull List<Territory> territories) {
        // assume no territory deletion
        if (territories.isEmpty()) {
            return true;
        }

        String placeHolder = "(?, ?, ?, ?, ?, ?, ?, ?)";
        return this.execute(
                "INSERT INTO `territory` (`name`, `guild_name`, `acquired`, `attacker`, `start_x`, `start_z`, `end_x`, `end_z`) " +
                        "VALUES " + String.join(", ", Collections.nCopies(territories.size(), placeHolder)) +
                        " ON DUPLICATE KEY UPDATE `guild_name` = VALUES(`guild_name`), `acquired` = VALUES(`acquired`), `attacker` = VALUES(`attacker`), " +
                        "`start_x` = VALUES(`start_x`), `start_z` = VALUES(`start_z`), `end_x` = VALUES(`end_x`), `end_z` = VALUES(`end_z`)",
                territories.stream().flatMap(t -> {
                    Territory.Location location = t.getLocation();
                    return Stream.of(
                            t.getName(),
                            t.getGuild(),
                            dbFormat.format(t.getAcquired()),
                            t.getAttacker(),
                            location.getStartX(),
                            location.getStartZ(),
                            location.getEndX(),
                            location.getEndZ()
                    );
                }).toArray()
        );
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

    @Nullable
    @Override
    public List<Territory> findAllIn(List<String> territoryNames) {
        String placeHolder = "?";
        ResultSet res = this.executeQuery(
                "SELECT * FROM `territory` WHERE `name` IN (" +
                        territoryNames.stream().map(t -> placeHolder).collect(Collectors.joining(", "))
                        + ")",
                territoryNames.toArray()
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
    public boolean delete(@NotNull TerritoryId territoryId) {
        return this.execute(
                "DELETE FROM `territory` WHERE `name` = ?",
                territoryId.getName()
        );
    }
}
