package db.repository.base;

import db.ConnectionPool;
import db.model.guildList.GuildListEntry;
import db.model.guildList.GuildListEntryId;
import log.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public abstract class GuildListRepository extends Repository<GuildListEntry, GuildListEntryId> {
    protected GuildListRepository(ConnectionPool db, Logger logger) {
        super(db, logger);
    }

    /**
     * Get user list name -> number of entries map.
     * @param userId User ID.
     * @return User defined guild lists.
     */
    @Nullable
    public abstract Map<String, Integer> getUserLists(long userId);

    /**
     * Retrieves the guild list of the user.
     * @param userId User ID.
     * @param listName List name.
     * @return User defined guild list.
     */
    @Nullable
    public abstract List<GuildListEntry> getList(long userId, @NotNull String listName);
}
