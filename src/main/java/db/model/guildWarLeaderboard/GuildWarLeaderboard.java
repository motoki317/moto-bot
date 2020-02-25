package db.model.guildWarLeaderboard;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;

public class GuildWarLeaderboard implements GuildWarLeaderboardId {
    @NotNull
    private String guildName;
    private int totalWar;
    private int successWar;

    // generated column
    private double successRate;

    public GuildWarLeaderboard(@NotNull String guildName, int totalWar, int successWar, @Nullable BigDecimal successRate) {
        this.guildName = guildName;
        this.totalWar = totalWar;
        this.successWar = successWar;
        this.successRate = successRate != null ?
                successRate.doubleValue()
                : (double) successWar / (double) totalWar;
    }

    @NotNull
    public String getGuildName() {
        return guildName;
    }

    public int getTotalWar() {
        return totalWar;
    }

    public int getSuccessWar() {
        return successWar;
    }

    public double getSuccessRate() {
        return successRate;
    }
}
