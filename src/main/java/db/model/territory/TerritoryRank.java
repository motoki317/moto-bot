package db.model.territory;

public class TerritoryRank {
    private final String guildName;
    private final int count;
    private final int rank;

    public TerritoryRank(String guildName, int count, int rank) {
        this.guildName = guildName;
        this.count = count;
        this.rank = rank;
    }

    public String getGuildName() {
        return guildName;
    }

    public int getCount() {
        return count;
    }

    public int getRank() {
        return rank;
    }
}
