package db.repository.mariadb;

import db.ConnectionPool;
import db.model.dateFormat.CustomDateFormat;
import db.model.dateFormat.CustomDateFormatId;
import db.model.dateFormat.CustomFormat;
import db.repository.base.DateFormatRepository;
import log.Logger;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

class MariaDateFormatRepository extends MariaRepository<CustomDateFormat> implements DateFormatRepository {
    MariaDateFormatRepository(ConnectionPool db, Logger logger) {
        super(db, logger);
    }

    @Override
    protected CustomDateFormat bind(@NotNull ResultSet res) throws SQLException {
        return new CustomDateFormat(res.getLong(1), CustomFormat.valueOf(res.getString(2)));
    }

    @Override
    public <S extends CustomDateFormat> boolean create(@NotNull S entity) {
        return this.execute(
                "INSERT INTO `date_format` (discord_id, date_format) VALUES (?, ?)",
                entity.getDiscordId(),
                entity.getDateFormat()
        );
    }

    @Override
    public boolean exists(@NotNull CustomDateFormatId customDateFormatId) {
        ResultSet res = this.executeQuery(
                "SELECT COUNT(*) FROM `date_format` WHERE `discord_id` = ?",
                customDateFormatId.getDiscordId()
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
                "SELECT COUNT(*) FROM `date_format`"
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
    public CustomDateFormat findOne(@NotNull CustomDateFormatId customDateFormatId) {
        ResultSet res = this.executeQuery(
                "SELECT * FROM `date_format` WHERE `discord_id` = ?",
                customDateFormatId.getDiscordId()
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

    @NotNull
    public CustomDateFormat getDateFormat(long... ids) {
        CustomDateFormat ret = CustomDateFormat.getDefault();
        for (long id : ids) {
            CustomDateFormat format = this.findOne(() -> id);
            if (format != null) {
                ret = format;
            }
        }
        return ret;
    }

    @NotNull
    public CustomDateFormat getDateFormat(MessageReceivedEvent event) {
        if (event.isFromGuild()) {
            return this.getDateFormat(
                    event.getGuild().getIdLong(),
                    event.getChannel().getIdLong(),
                    event.getAuthor().getIdLong()
            );
        } else {
            return this.getDateFormat(
                    event.getChannel().getIdLong(),
                    event.getAuthor().getIdLong()
            );
        }
    }

    @Nullable
    @Override
    public List<CustomDateFormat> findAll() {
        ResultSet res = this.executeQuery(
                "SELECT * FROM `date_format`"
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
    public boolean update(@NotNull CustomDateFormat entity) {
        return this.execute(
                "UPDATE `date_format` SET `date_format` = ? WHERE `discord_id` = ?",
                entity.getDateFormat(),
                entity.getDiscordId()
        );
    }

    @Override
    public boolean delete(@NotNull CustomDateFormatId customDateFormatId) {
        return this.execute(
                "DELETE FROM `date_format` WHERE `discord_id` = ?",
                customDateFormatId.getDiscordId()
        );
    }
}
