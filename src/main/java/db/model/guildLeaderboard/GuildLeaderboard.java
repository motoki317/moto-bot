package db.model.guildLeaderboard;

import java.util.Date;
import java.util.Objects;

public class GuildLeaderboard implements GuildLeaderboardId {
    private final String name;
    private final String prefix;
    private final long xp;
    private final int level;
    private final int num;
    private final int territories;
    private final int memberCount;
    private final Date updatedAt;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GuildLeaderboard that = (GuildLeaderboard) o;
        return xp == that.xp &&
                level == that.level &&
                num == that.num &&
                territories == that.territories &&
                memberCount == that.memberCount &&
                name.equals(that.name) &&
                prefix.equals(that.prefix) &&
                updatedAt.equals(that.updatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, prefix, xp, level, num, territories, memberCount, updatedAt);
    }

    public int compareLevelAndXP(GuildLeaderboard other) {
        if (this.level < other.level) {
            return -1;
        } else if (this.level > other.level) {
            return 1;
        }

        return Long.compare(this.xp, other.xp);
    }
}
