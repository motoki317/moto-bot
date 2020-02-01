package db.model.territory;

public class TerritoryRank {
    private String guildName;
    private int count;
    private int rank;

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
