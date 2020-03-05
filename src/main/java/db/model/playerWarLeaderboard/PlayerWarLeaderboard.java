package db.model.playerWarLeaderboard;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;

public class PlayerWarLeaderboard implements PlayerWarLeaderboardId {
    @NotNull
    private String UUID;
    @NotNull
    private String lastName;
    private int totalWar;
    private int successWar;
    private int survivedWar;
    private double successRate;
    private double survivedRate;

    public PlayerWarLeaderboard(@NotNull String UUID, @NotNull String lastName, int totalWar, int successWar, int survivedWar,
                                @Nullable BigDecimal successRate, @Nullable BigDecimal survivedRate) {
        this.UUID = UUID;
        this.lastName = lastName;
        this.totalWar = totalWar;
        this.successWar = successWar;
        this.survivedWar = survivedWar;
        this.successRate = successRate == null
                ? (double) successWar / (double) totalWar
                : successRate.doubleValue();
        this.survivedRate = survivedRate == null
                ? (double) survivedWar / (double) totalWar
                : survivedRate.doubleValue();
    }

    @NotNull
    public String getUUID() {
        return UUID;
    }

    @NotNull
    public String getLastName() {
        return lastName;
    }

    public int getTotalWar() {
        return totalWar;
    }

    public int getSuccessWar() {
        return successWar;
    }

    public int getSurvivedWar() {
        return survivedWar;
    }

    public double getSuccessRate() {
        return successRate;
    }

    public double getSurvivedRate() {
        return survivedRate;
    }
}
