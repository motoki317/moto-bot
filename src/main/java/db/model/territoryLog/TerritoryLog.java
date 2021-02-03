package db.model.territoryLog;

import java.util.Date;

public class TerritoryLog implements TerritoryLogId {
    private final int id;

    private final String territoryName;

    private final String oldGuildName;
    private final String newGuildName;

    private final int oldGuildTerrAmt;
    private final int newGuildTerrAmt;

    private final Date acquired;
    private final long timeDiff;

    public TerritoryLog(int id, String territoryName, String oldGuildName, String newGuildName,
                        int oldGuildTerrAmt, int newGuildTerrAmt, Date acquired, long timeDiff) {
        this.id = id;
        this.territoryName = territoryName;
        this.oldGuildName = oldGuildName;
        this.newGuildName = newGuildName;
        this.oldGuildTerrAmt = oldGuildTerrAmt;
        this.newGuildTerrAmt = newGuildTerrAmt;
        this.acquired = acquired;
        this.timeDiff = timeDiff;
    }

    @Override
    public int getId() {
        return id;
    }

    public String getTerritoryName() {
        return territoryName;
    }

    public String getOldGuildName() {
        return oldGuildName;
    }

    public String getNewGuildName() {
        return newGuildName;
    }

    public int getOldGuildTerrAmt() {
        return oldGuildTerrAmt;
    }

    public int getNewGuildTerrAmt() {
        return newGuildTerrAmt;
    }

    public Date getAcquired() {
        return acquired;
    }

    public long getTimeDiff() {
        return timeDiff;
    }
}
