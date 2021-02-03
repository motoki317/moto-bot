package db.repository.base;

import db.model.territoryList.TerritoryListEntry;
import db.model.territoryList.TerritoryListEntryId;
import db.repository.Repository;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public interface TerritoryListRepository extends Repository<TerritoryListEntry, TerritoryListEntryId> {
    /**
     * Get user list name -> number of entries map.
     * @param userId User ID.
     * @return User defined territory lists.
     */
    @Nullable Map<String, Integer> getUserLists(long userId);

    /**
     * Retrieves the territory list of the user.
     * @param userId User ID.
     * @param listName List name.
     * @return User defined territory list.
     */
    @Nullable List<TerritoryListEntry> getList(long userId, @NotNull String listName);
}
