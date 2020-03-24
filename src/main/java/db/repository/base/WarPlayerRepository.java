package db.repository.base;

import db.ConnectionPool;
import db.model.warPlayer.WarPlayer;
import db.model.warPlayer.WarPlayerId;
import log.Logger;
import utils.UUID;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.List;

public abstract class WarPlayerRepository extends Repository<WarPlayer, WarPlayerId> {
    protected WarPlayerRepository(ConnectionPool db, Logger logger) {
        super(db, logger);
    }

    /**
     * Finds all player logs of the given war log id.
     * @param warLogId War log id.
     * @return List of war players. null if something went wrong.
     */
    @Nullable
    public abstract List<WarPlayer> findAllOfWarLogId(int warLogId);

    /**
     * Returns the count this player has participated in the war.
     * @param playerUUID UUID to search with.
     * @return Count. -1 if something went wrong.
     */
    public abstract int countOfPlayer(UUID playerUUID);

    /**
     * Gets count of success wars by this player.
     * @param playerUUID Player UUID.
     * @return Count of success wars. -1 if something went wrong.
     */
    public abstract int countSuccessWars(UUID playerUUID);

    /**
     * Gets count of survived wars by this player.
     * @param playerUUID Player UUID.
     * @return Count of survived wars. -1 if something went wrong.
     */
    public abstract int countSurvivedWars(UUID playerUUID);

    /**
     * Retrieves player logs in this table by descending order of war_log_id.
     * @param playerUUID Player UUID.
     * @param limit Retrieval limit.
     * @param offset Retrieval offset.
     * @return List of logs. null if something went wrong.
     */
    @Nullable
    public abstract List<WarPlayer> getLogsOfPlayer(UUID playerUUID, int limit, int offset);

    /**
     * Retrieves single player whose logged UUID is null.
     * @return War player entry. null if something went wrong or not found.
     */
    @Nullable
    public abstract WarPlayer getUUIDNullPlayer(int offset);

    /**
     * Updates list of war player entries of player name between the given dates, to given player uuid.
     * @param playerName Player name.
     * @param uuid Player UUID to update to.
     * @param start Start date (inclusive).
     * @param end End date (exclusive).
     * @return {@code true} if success.
     */
    public abstract boolean updatePlayerNameOfBetween(String playerName, UUID uuid, Date start, Date end);
}
