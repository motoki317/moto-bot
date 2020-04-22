package db.repository.mariadb;

import db.ConnectionPool;
import db.model.musicInterruptedGuild.MusicInterruptedGuild;
import db.model.musicInterruptedGuild.MusicInterruptedGuildId;
import db.repository.base.MusicInterruptedGuildRepository;
import log.Logger;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

class MariaMusicInterruptedGuildRepository extends MusicInterruptedGuildRepository {
    MariaMusicInterruptedGuildRepository(ConnectionPool db, Logger logger) {
        super(db, logger);
    }

    @Override
    protected MusicInterruptedGuild bind(@NotNull ResultSet res) throws SQLException {
        return new MusicInterruptedGuild(res.getLong(1), res.getLong(2), res.getLong(3));
    }

    @Override
    public <S extends MusicInterruptedGuild> boolean create(@NotNull S entity) {
        return this.execute(
                "INSERT INTO `music_interrupted_guild` (guild_id, channel_id, voice_channel_id) VALUES (?, ?, ?)",
                entity.getGuildId(),
                entity.getChannelId(),
                entity.getVoiceChannelId()
        );
    }

    @Override
    public boolean createAll(List<MusicInterruptedGuild> guilds) {
        if (guilds.isEmpty()) {
            return true;
        }
        return this.execute(
                "INSERT INTO `music_interrupted_guild` (guild_id, channel_id, voice_channel_id) VALUES " +
                        String.join(", ", Collections.nCopies(guilds.size(), "(?, ?, ?)")),
                guilds.stream().flatMap(g -> Stream.of(
                        g.getGuildId(),
                        g.getChannelId(),
                        g.getVoiceChannelId()
                )).toArray()
        );
    }

    @Override
    public boolean deleteAll() {
        return this.execute(
                "TRUNCATE TABLE `music_interrupted_guild`"
        );
    }

    @Override
    public boolean exists(@NotNull MusicInterruptedGuildId musicInterruptedGuildId) {
        ResultSet res = this.executeQuery(
                "SELECT COUNT(*) FROM `music_interrupted_guild` WHERE guild_id = ?",
                musicInterruptedGuildId.getGuildId()
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
                "SELECT COUNT(*) FROM `music_interrupted_guild`"
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
    public MusicInterruptedGuild findOne(@NotNull MusicInterruptedGuildId musicInterruptedGuildId) {
        ResultSet res = this.executeQuery(
                "SELECT * FROM `music_interrupted_guild` WHERE guild_id = ?",
                musicInterruptedGuildId.getGuildId()
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
    public List<MusicInterruptedGuild> findAll() {
        ResultSet res = this.executeQuery(
                "SELECT * FROM `music_interrupted_guild`"
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
    public boolean update(@NotNull MusicInterruptedGuild entity) {
        return this.execute(
                "UPDATE `music_interrupted_guild` SET `channel_id` = ?, `voice_channel_id` = ? WHERE `guild_id` = ?",
                entity.getChannelId(),
                entity.getVoiceChannelId(),
                entity.getGuildId()
        );
    }

    @Override
    public boolean delete(@NotNull MusicInterruptedGuildId musicInterruptedGuildId) {
        return this.execute(
                "DELETE FROM `music_interrupted_guild` WHERE `guild_id` = ?",
                musicInterruptedGuildId.getGuildId()
        );
    }
}
