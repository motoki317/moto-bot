package db.repository.base;

import db.ConnectionPool;
import db.model.musicSetting.MusicSetting;
import db.model.musicSetting.MusicSettingId;
import log.Logger;

public abstract class MusicSettingRepository extends Repository<MusicSetting, MusicSettingId> {
    protected MusicSettingRepository(ConnectionPool db, Logger logger) {
        super(db, logger);
    }
}
