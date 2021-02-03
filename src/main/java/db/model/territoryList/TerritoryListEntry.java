package db.model.territoryList;

import org.jetbrains.annotations.NotNull;

public class TerritoryListEntry implements TerritoryListEntryId {
    private final long userId;
    @NotNull
    private final String listName;
    @NotNull
    private final String territoryName;

    public TerritoryListEntry(long userId, @NotNull String listName, @NotNull String territoryName) {
        this.userId = userId;
        this.listName = listName;
        this.territoryName = territoryName;
    }

    @Override
    public long getUserId() {
        return userId;
    }

    @NotNull
    @Override
    public String getListName() {
        return listName;
    }

    @NotNull
    @Override
    public String getTerritoryName() {
        return territoryName;
    }
}
