package db.repository;

import db.ConnectionPool;
import db.model.world.World;
import db.model.world.WorldId;
import db.repository.base.Repository;
import log.Logger;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class WorldRepository extends Repository<World, WorldId> {
    public WorldRepository(ConnectionPool db, Logger logger) {
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
    public @NotNull List<World> findAll() {
        ResultSet res = this.executeQuery("SELECT * FROM `world`");

        if (res == null) return new ArrayList<>();

        try {
            return bindAll(res);
        } catch (SQLException e) {
            this.logResponseException(e);
        }
        return new ArrayList<>();
    }

    /**
     * Retrieves all main worlds (WC.* or EU.*).
     * @return List of all main worlds.
     */
    public @NotNull List<World> findAllMainWorlds() {
        ResultSet res = this.executeQuery("SELECT * FROM `world` WHERE `name` LIKE 'WC%' OR `name` LIKE 'EU%'");

        if (res == null) return new ArrayList<>();

        try {
            return bindAll(res);
        } catch (SQLException e) {
            this.logResponseException(e);
        }
        return new ArrayList<>();
    }

    @Override
    public boolean update(@NotNull World entity) {
        return this.execute(
                "UPDATE `world` SET `players` = ?, `updated_at` = NOW() WHERE `name` = ?",
                entity.getPlayers(),
                entity.getName()
        );
    }

    /**
     * Updates all worlds to the given worlds, and removes all worlds not in the given worlds.
     * @param worlds Current list of worlds.
     * @return True if success.
     */
    public boolean updateAll(Collection<World> worlds) {
        Connection connection = this.db.getConnection();
        if (connection == null) {
            return false;
        }

        try {
            connection.setAutoCommit(false);

            // TODO: migrate logic from player tracker

            return true;
        } catch (SQLException e) {
            this.logger.logException("an exception occurred while updating worlds.", e);
            this.logger.log(0, "Rolling back world update changes.");
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
    public boolean delete(@NotNull WorldId worldId) {
        return this.execute(
                "DELETE FROM `world` WHERE `name` = ?",
                worldId.getName()
        );
    }
}
