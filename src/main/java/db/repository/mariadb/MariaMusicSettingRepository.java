package db.repository.mariadb;

import db.ConnectionPool;
import db.model.musicSetting.MusicSetting;
import db.model.musicSetting.MusicSettingId;
import db.repository.base.MusicSettingRepository;
import log.Logger;
import music.RepeatState;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

class MariaMusicSettingRepository extends MariaRepository<MusicSetting> implements MusicSettingRepository {
    MariaMusicSettingRepository(ConnectionPool db, Logger logger) {
        super(db, logger);
    }

    @Override
    protected MusicSetting bind(@NotNull ResultSet res) throws SQLException {
        long restrictChannel;
        return new MusicSetting(
                res.getLong(1),
                res.getInt(2),
                RepeatState.valueOf(res.getString(3)),
                res.getBoolean(4),
                (restrictChannel = res.getLong(5)) != 0 ? restrictChannel : null
        );
    }

    @Override
    public <S extends MusicSetting> boolean create(@NotNull S entity) {
        return this.execute(
                "INSERT INTO `music_setting` (guild_id, volume, `repeat`, show_np, restrict_channel) VALUES (?, ?, ?, ?, ?)",
                entity.getGuildId(),
                entity.getVolume(),
                entity.getRepeat(),
                entity.isShowNp() ? 1 : 0,
                entity.getRestrictChannel()
        );
    }

    @Override
    public boolean exists(@NotNull MusicSettingId musicSettingId) {
        ResultSet res = this.executeQuery(
                "SELECT COUNT(*) FROM `music_setting` WHERE `guild_id` = ?",
                musicSettingId.getGuildId()
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
                "SELECT COUNT(*) FROM `music_setting`"
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
    public MusicSetting findOne(@NotNull MusicSettingId musicSettingId) {
        ResultSet res = this.executeQuery(
                "SELECT * FROM `music_setting` WHERE `guild_id` = ?",
                musicSettingId.getGuildId()
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
    public List<MusicSetting> findAll() {
        ResultSet res = this.executeQuery(
                "SELECT * FROM `music_setting`"
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
    public boolean update(@NotNull MusicSetting entity) {
        return this.execute(
                "UPDATE `music_setting` SET volume = ?, `repeat` = ?, show_np = ?, restrict_channel = ? WHERE `guild_id` = ?",
                entity.getVolume(),
                entity.getRepeat(),
                entity.isShowNp() ? 1 : 0,
                entity.getRestrictChannel(),
                entity.getGuildId()
        );
    }

    @Override
    public boolean delete(@NotNull MusicSettingId musicSettingId) {
        return this.execute(
                "DELETE FROM `music_setting` WHERE `guild_id` = ?",
                musicSettingId.getGuildId()
        );
    }
}
