package db.model.guildXpLeaderboard;

import java.util.Date;

public class GuildXpLeaderboard implements GuildXpLeaderboardId {
    private String name;
    private String prefix;
    private int level;
    private long xp;
    private long xpDiff;
    private Date from;
    private Date to;

    public GuildXpLeaderboard(String name, String prefix, int level, long xp, long xpDiff, Date from, Date to) {
        this.name = name;
        this.prefix = prefix;
        this.level = level;
        this.xp = xp;
        this.xpDiff = xpDiff;
        this.from = from;
        this.to = to;
    }

    public String getName() {
        return name;
    }

    public String getPrefix() {
        return prefix;
    }

    public int getLevel() {
        return level;
    }

    public long getXp() {
        return xp;
    }

    public long getXpDiff() {
        return xpDiff;
    }

    public Date getFrom() {
        return from;
    }

    public Date getTo() {
        return to;
    }
}
