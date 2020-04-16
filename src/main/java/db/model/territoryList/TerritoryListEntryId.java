package db.model.territoryList;

import org.jetbrains.annotations.NotNull;

public interface TerritoryListEntryId {
    long getUserId();
    @NotNull
    String getListName();
    @NotNull
    String getTerritoryName();
}
