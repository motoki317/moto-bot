package db.repository.base;

import db.model.guildList.GuildListEntry;
import db.model.guildList.GuildListEntryId;
import db.repository.Repository;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public interface GuildListRepository extends Repository<GuildListEntry, GuildListEntryId> {
    /**
     * Get user list name -> number of entries map.
     * @param userId User ID.
     * @return User defined guild lists.
     */
    @Nullable Map<String, Integer> getUserLists(long userId);

    /**
     * Retrieves the guild list of the user.
     * @param userId User ID.
     * @param listName List name.
     * @return User defined guild list.
     */
    @Nullable List<GuildListEntry> getList(long userId, @NotNull String listName);
}
