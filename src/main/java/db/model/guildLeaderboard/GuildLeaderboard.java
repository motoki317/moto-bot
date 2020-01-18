package db.model.guildLeaderboard;

import java.util.Date;

public class GuildLeaderboard implements GuildLeaderboardId {
    private String name;
    private String prefix;
    private long xp;
    private int level;
    private int num;
    private int territories;
    private int memberCount;
    private Date updatedAt;

    public GuildLeaderboard(String name, String prefix, long xp, int level, int num, int territories, int memberCount, Date updatedAt) {
        this.name = name;
        this.prefix = prefix;
        this.xp = xp;
        this.level = level;
        this.num = num;
        this.territories = territories;
        this.memberCount = memberCount;
        this.updatedAt = updatedAt;
    }

    public String getName() {
        return name;
    }

    public String getPrefix() {
        return prefix;
    }

    public long getXp() {
        return xp;
    }

    public int getLevel() {
        return level;
    }

    public int getNum() {
        return num;
    }

    public int getTerritories() {
        return territories;
    }

    public int getMemberCount() {
        return memberCount;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }
}
