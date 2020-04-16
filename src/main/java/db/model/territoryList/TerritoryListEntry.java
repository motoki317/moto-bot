package db.model.territoryList;

import org.jetbrains.annotations.NotNull;

public class TerritoryListEntry implements TerritoryListEntryId {
    private long userId;
    @NotNull
    private String listName;
    @NotNull
    private String territoryName;

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
