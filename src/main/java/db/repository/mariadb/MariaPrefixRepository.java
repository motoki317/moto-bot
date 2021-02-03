package db.repository.mariadb;

import db.ConnectionPool;
import db.model.prefix.Prefix;
import db.model.prefix.PrefixId;
import db.repository.base.PrefixRepository;
import log.Logger;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

class MariaPrefixRepository extends MariaRepository<Prefix> implements PrefixRepository {
    MariaPrefixRepository(ConnectionPool db, Logger logger) {
        super(db, logger);
    }

    @Override
    protected Prefix bind(@NotNull ResultSet res) throws SQLException {
        return new Prefix(res.getLong(1), res.getString(2));
    }

    @Override
    public <S extends Prefix> boolean create(@NotNull S entity) {
        return this.execute(
                "INSERT INTO `prefix` (`discord_id`, `prefix`) VALUES (?, ?)",
                entity.getDiscordId(),
                entity.getPrefix()
        );
    }

    @Override
    public boolean exists(@NotNull PrefixId prefixId) {
        ResultSet res = this.executeQuery(
                "SELECT COUNT(*) FROM `prefix` WHERE `discord_id` = ?",
                prefixId.getDiscordId()
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
                "SELECT COUNT(*) FROM `prefix`"
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
    public Prefix findOne(@NotNull PrefixId prefixId) {
        ResultSet res = this.executeQuery(
                "SELECT * FROM `prefix` WHERE `discord_id` = ?",
                prefixId.getDiscordId()
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
    public List<Prefix> findAll() {
        ResultSet res = this.executeQuery(
                "SELECT * FROM `prefix`"
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
    public boolean update(@NotNull Prefix entity) {
        return this.execute(
                "UPDATE `prefix` SET `prefix` = ? WHERE `discord_id` = ?",
                entity.getPrefix(),
                entity.getDiscordId()
        );
    }

    @Override
    public boolean delete(@NotNull PrefixId prefixId) {
        return this.execute(
                "DELETE FROM `prefix` WHERE `discord_id` = ?",
                prefixId.getDiscordId()
        );
    }
}
