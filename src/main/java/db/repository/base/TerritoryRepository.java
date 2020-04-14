package db.repository.base;

import db.ConnectionPool;
import db.model.territory.Territory;
import db.model.territory.TerritoryId;
import db.model.territory.TerritoryRank;
import log.Logger;
import org.jetbrains.annotations.NotNull;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.util.Date;
import java.util.List;

public abstract class TerritoryRepository extends Repository<Territory, TerritoryId> {
    protected TerritoryRepository(ConnectionPool db, Logger logger) {
        super(db, logger);
    }

    /**
     * Counts number of guild territories a guild possesses.
     * @param guildName Guild name.
     * @return Territory count. -1 if something went wrong.
     */
    public abstract int countGuildTerritories(@NotNull String guildName);

    /**
     * Returns a list of territories the guild possesses.
     * @param guildName Guild name.
     * @return Territory list.
     */
    @Nullable
    public abstract List<Territory> getGuildTerritories(@NotNull String guildName);

    /**
     * Get ranking of guilds by number of territories.
     * @return Ranking map, where keys are guild names and values are number of territories.
     */
    @Nullable
    public abstract List<TerritoryRank> getGuildTerritoryNumbers();

    /**
     * Get ranking of guild by number of territories.
     * @param guildName Guild name.
     * @return Ranking. -1 if something went wrong. 0 if the guild does not exist in the ranking.
     */
    public abstract int getGuildTerritoryRanking(@NotNull String guildName);

    /**
     * Retrieves the latest `acquired` time.
     * @return Latest territory acquired time.
     */
    @Nullable
    public abstract Date getLatestAcquiredTime();

    /**
     * Updates the whole table to the new given territories list.
     * @param territories New territories list retrieved from the Wynn API.
     * @return {@code true} if succeeded.
     */
    @CheckReturnValue
    public abstract boolean updateAll(@NotNull List<Territory> territories);

    /**
     * Retrieves all territory names that begins with the given prefix. Case insensitive.
     * @param prefix Prefix.
     * @return List of territory names.
     */
    @Nullable
    public abstract List<String> territoryNamesBeginsWith(String prefix);
}
