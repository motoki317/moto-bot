package db.model.territoryLog;

import org.jetbrains.annotations.NotNull;

public class TerritoryActivity {
    @NotNull
    private final String territoryName;
    private final int count;

    public TerritoryActivity(@NotNull String territoryName, int count) {
        this.territoryName = territoryName;
        this.count = count;
    }

    @NotNull
    public String getTerritoryName() {
        return territoryName;
    }

    public int getCount() {
        return count;
    }
}
