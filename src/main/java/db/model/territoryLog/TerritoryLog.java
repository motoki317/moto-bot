package db.model.territoryLog;

import java.util.Date;

public class TerritoryLog implements TerritoryLogId {
    private int id;

    private String territoryName;

    private String oldGuildName;
    private String newGuildName;

    private int oldGuildTerrAmt;
    private int newGuildTerrAmt;

    private Date acquired;
    private long timeDiff;

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
