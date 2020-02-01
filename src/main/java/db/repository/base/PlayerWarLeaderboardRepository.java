package db.repository.base;

import db.ConnectionPool;
import db.model.playerWarLeaderboard.PlayerWarLeaderboard;
import db.model.playerWarLeaderboard.PlayerWarLeaderboardId;
import log.Logger;

public abstract class PlayerWarLeaderboardRepository extends Repository<PlayerWarLeaderboard, PlayerWarLeaderboardId> {
    protected PlayerWarLeaderboardRepository(ConnectionPool db, Logger logger) {
        super(db, logger);
    }
}
