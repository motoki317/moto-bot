package db.repository.mariadb;

import db.ConnectionPool;
import db.model.warLog.WarLog;
import db.model.warLog.WarLogId;
import db.model.warPlayer.WarPlayer;
import db.repository.base.WarLogRepository;
import log.Logger;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

class MariaWarLogRepository extends MariaRepository<WarLog> implements WarLogRepository {
    private static final DateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private final MariaWarPlayerRepository warPlayerRepository;

    MariaWarLogRepository(ConnectionPool db, Logger logger, MariaWarPlayerRepository warPlayerRepository) {
        super(db, logger);
        this.warPlayerRepository = warPlayerRepository;
    }

    @Override
    protected WarLog bind(@NotNull ResultSet res) throws SQLException {
        List<WarPlayer> players = this.warPlayerRepository.findAllOfWarLogId(res.getInt(1));
        if (players == null) {
            throw new SQLException("Failed to retrieve war players");
        }

        return new WarLog(res.getInt(1), res.getString(2), res.getString(3),
                res.getTimestamp(4), res.getTimestamp(5), res.getBoolean(6), res.getBoolean(7),
                players);
    }

    @Override
    public <S extends WarLog> boolean create(@NotNull S entity) {
        int id = this.createAndGetLastInsertId(entity);
        return id != 0;
    }

    public int createAndGetLastInsertId(@NotNull WarLog entity) {
        Connection connection = this.db.getConnection();
        if (connection == null) {
            return 0;
        }

        try {
            connection.setAutoCommit(false);

            boolean res = this.execute(connection,
                    "INSERT INTO `war_log` (server_name, guild_name, created_at, last_up, ended, log_ended) VALUES (?, ?, ?, ?, ?, ?)",
                    entity.getServerName(),
                    entity.getGuildName(),
                    dbFormat.format(entity.getCreatedAt()),
                    dbFormat.format(entity.getLastUp()),
                    entity.isEnded() ? 1 : 0,
                    entity.isLogEnded() ? 1 : 0
            );
            if (!res) {
                throw new SQLException("Failed to insert into war_log");
            }

            int lastInsertId = this.lastInsertId(connection);
            if (lastInsertId == 0) {
                throw new SQLException("Failed to get last insert id");
            }

            for (WarPlayer player : entity.getPlayers()) {
                player.setWarLogId(lastInsertId);
                res = this.warPlayerRepository.create(connection, player);
                if (!res) {
                    throw new SQLException("Failed to insert into war_player");
                }
            }
            return lastInsertId;
        } catch (SQLException e) {
            this.logResponseException(e);
            try {
                connection.rollback();
            } catch (SQLException ex) {
                this.logger.logException("Something went wrong while rolling back changes", ex);
            }
            return 0;
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                this.logger.logException("Something went wrong while setting auto commit back on", e);
            }

            this.db.releaseConnection(connection);
        }
    }

    @Override
    public boolean exists(@NotNull WarLogId warLogId) {
        ResultSet res = this.executeQuery(
                "SELECT COUNT(*) FROM `war_log` WHERE `id` = ?",
                warLogId.getId()
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
                "SELECT COUNT(*) FROM `war_log`"
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

    /**
     * Retrieves the last insert id (MAX(id)) of this table.
     * @return Last insert id. 0 if something went wrong.
     */
    private int lastInsertId(Connection connection) {
        ResultSet res = this.executeQuery(connection,
                "SELECT MAX(`id`) FROM `war_log`"
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
    public WarLog findOne(@NotNull WarLogId warLogId) {
        ResultSet res = this.executeQuery(
                "SELECT * FROM `war_log` WHERE `id` = ?",
                warLogId.getId()
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
    public List<WarLog> findAllIn(List<Integer> ids) {
        if (ids.isEmpty()) {
            return new ArrayList<>();
        }

        String placeHolder = String.format("(%s)",
                ids.stream().map(i -> "?").collect(Collectors.joining(", "))
        );
        ResultSet res = this.executeQuery(
                "SELECT * FROM `war_log` WHERE `id` IN " + placeHolder,
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

    @Nullable
    @Override
    public List<WarLog> findAllNotEnded() {
        ResultSet res = this.executeQuery(
                "SELECT * FROM `war_log` WHERE `ended` = 0"
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

    @Nullable
    @Override
    public List<WarLog> findAll() {
        throw new Error("Find all not implemented: number of records could be huge. Use limit, offset, where clauses instead.");
    }

    @Nullable
    public List<WarLog> findAllLogNotEnded() {
        ResultSet res = this.executeQuery(
                "SELECT * FROM `war_log` WHERE `log_ended` = 0"
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
    public boolean update(@NotNull WarLog entity) {
        Connection connection = this.db.getConnection();
        if (connection == null) {
            return false;
        }

        try {
            connection.setAutoCommit(false);

            boolean res = this.execute(
                    "UPDATE `war_log` SET `server_name` = ?, `guild_name` = ?, `created_at` = ?, `last_up` = ?, `ended` = ?, `log_ended` = ? WHERE `id` = ?",
                    entity.getServerName(),
                    entity.getGuildName(),
                    dbFormat.format(entity.getCreatedAt()),
                    dbFormat.format(entity.getLastUp()),
                    entity.isEnded() ? 1 : 0,
                    entity.isLogEnded() ? 1 : 0,
                    entity.getId()
            );
            if (!res) {
                throw new SQLException("Failed to update war_log");
            }

            for (WarPlayer player : entity.getPlayers()) {
                if (this.warPlayerRepository.exists(player)) {
                    res = this.warPlayerRepository.update(connection, player);
                    if (!res) {
                        throw new SQLException("Failed to update war_player");
                    }
                } else {
                    res = this.warPlayerRepository.create(connection, player);
                    if (!res) {
                        throw new SQLException("Failed to create war_player");
                    }
                }
            }
            return true;
        } catch (SQLException e) {
            this.logResponseException(e);
            try {
                connection.rollback();
            } catch (SQLException ex) {
                this.logger.logException("Something went wrong while rolling back changes", ex);
            }
            return false;
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                this.logger.logException("Something went wrong while setting auto commit back on", e);
            }

            this.db.releaseConnection(connection);
        }
    }

    @Override
    public boolean delete(@NotNull WarLogId warLogId) {
        return this.execute(
                "DELETE FROM `war_log` WHERE `id` = ?",
                warLogId.getId()
        );
    }
}
