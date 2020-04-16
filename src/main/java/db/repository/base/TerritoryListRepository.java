package db.repository.base;

import db.ConnectionPool;
import db.model.territoryList.TerritoryListEntry;
import db.model.territoryList.TerritoryListEntryId;
import log.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public abstract class TerritoryListRepository extends Repository<TerritoryListEntry, TerritoryListEntryId> {
    protected TerritoryListRepository(ConnectionPool db, Logger logger) {
        super(db, logger);
    }

    /**
     * Get user list name -> number of entries map.
     * @param userId User ID.
     * @return User defined territory lists.
     */
    @Nullable
    public abstract Map<String, Integer> getUserLists(long userId);

    /**
     * Retrieves the territory list of the user.
     * @param userId User ID.
     * @param listName List name.
     * @return User defined territory list.
     */
    @Nullable
    public abstract List<TerritoryListEntry> getList(long userId, @NotNull String listName);
}
