package db.repository.mariadb;

import db.ConnectionPool;
import db.model.world.World;
import db.model.world.WorldId;
import db.repository.base.WorldRepository;
import log.Logger;
import org.jetbrains.annotations.NotNull;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class MariaWorldRepository extends WorldRepository {
    MariaWorldRepository(ConnectionPool db, Logger logger) {
        super(db, logger);
    }

    @Override
    protected World bind(@NotNull ResultSet res) throws SQLException {
        World instance = new World(res.getString(1), res.getInt(2));
        instance.setCreatedAt(res.getTimestamp(3));
        instance.setUpdatedAt(res.getTimestamp(4));
        return instance;
    }

    @Override
    public <S extends World> boolean create(@NotNull S entity) {
        return this.execute(
                "INSERT INTO `world` (`name`, `players`) VALUES (?, ?)",
                entity.getName(),
                entity.getPlayers()
        );
    }

    @Override
    public boolean exists(@NotNull WorldId worldId) {
        ResultSet res = this.executeQuery(
                "SELECT COUNT(*) FROM `world` WHERE `name` = ?",
                worldId.getName()
        );

        if (res == null) return false;

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
        ResultSet res = this.executeQuery("SELECT COUNT(*) FROM `world`");

        if (res == null) return 0;

        try {
            if (res.next())
                return res.getInt(1);
        } catch(SQLException e) {
            this.logResponseException(e);
        }
        return 0;
    }

    @Nullable
    @Override
    public World findOne(@NotNull WorldId worldId) {
        ResultSet res = this.executeQuery(
                "SELECT * FROM `world` WHERE `name` = ?",
                worldId.getName()
        );

        if (res == null) return null;

        try {
            if (res.next())
                return bind(res);
        } catch (SQLException e) {
            this.logResponseException(e);
        }
        return null;
    }

    @Override
    @Nullable
    public List<World> findAll() {
        ResultSet res = this.executeQuery("SELECT * FROM `world`");

        if (res == null) return null;

        try {
            return bindAll(res);
        } catch (SQLException e) {
            this.logResponseException(e);
        }
        return null;
    }

    /**
     * Retrieves all world names.
     * @return Set of world names.
     */
    @Nullable
    private Set<String> findAllWorldNames() {
        ResultSet res = this.executeQuery(
                "SELECT `name` FROM `world`"
        );

        if (res == null) {
            return null;
        }

        try {
            Set<String> worldNames = new HashSet<>();
            while (res.next()) {
                worldNames.add(res.getString(1));
            }
            return worldNames;
        } catch (SQLException e) {
            this.logResponseException(e);
            return null;
        }
    }

    @Nullable
    public List<World> findAllMainWorlds() {
        ResultSet res = this.executeQuery("SELECT * FROM `world` WHERE `name` LIKE 'WC%' OR `name` LIKE 'EU%'");

        if (res == null) return null;

        try {
            return bindAll(res);
        } catch (SQLException e) {
            this.logResponseException(e);
        }
        return null;
    }

    @Nullable
    @Override
    public List<World> findAllWarWorlds() {
        ResultSet res = this.executeQuery("SELECT * FROM `world` WHERE `name` LIKE 'WAR%'");

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
    public boolean update(@NotNull World entity) {
        return this.execute(
                "UPDATE `world` SET `players` = ?, `updated_at` = NOW() WHERE `name` = ?",
                entity.getPlayers(),
                entity.getName()
        );
    }

    @CheckReturnValue
    public boolean updateAll(Collection<World> worlds) {
        Set<String> oldWorldNames = this.findAllWorldNames();
        if (oldWorldNames == null) return false;

        Set<String> newWorldNames = worlds.stream().map(World::getName).collect(Collectors.toSet());
        Set<String> deletedWorldNames = oldWorldNames.stream().filter(w -> !newWorldNames.contains(w))
                .collect(Collectors.toSet());
        if (!deletedWorldNames.isEmpty()) {
            boolean res = this.execute(
                    "DELETE FROM `world` WHERE `name` IN (" +
                            String.join(", ", Collections.nCopies(deletedWorldNames.size(), "?")) +
                            ")",
                    deletedWorldNames.toArray()
            );
            if (!res) {
                return false;
            }
        }

        String placeHolder = "(?, ?)";
        return this.execute(
                "INSERT INTO `world` (name, players) VALUES " +
                        String.join(", ", Collections.nCopies(worlds.size(), placeHolder)) +
                        " ON DUPLICATE KEY UPDATE players = VALUES(players)",
                worlds.stream().flatMap(w -> Stream.of(
                        w.getName(),
                        w.getPlayers()
                )).toArray()
        );
    }

    @Override
    public boolean delete(@NotNull WorldId worldId) {
        return this.execute(
                "DELETE FROM `world` WHERE `name` = ?",
                worldId.getName()
        );
    }
}
