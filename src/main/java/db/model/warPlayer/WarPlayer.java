package db.model.warPlayer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WarPlayer implements WarPlayerId {
    private int warLogId;
    @NotNull
    private String playerName;
    @Nullable
    private String playerUUID;
    private boolean exited;

    public WarPlayer(int warLogId, @NotNull String playerName, @Nullable String playerUUID, boolean exited) {
        this.warLogId = warLogId;
        this.playerName = playerName;
        this.playerUUID = playerUUID;
        this.exited = exited;
    }

    public WarPlayer(@NotNull String playerName, @Nullable String playerUUID, boolean exited) {
        this.playerName = playerName;
        this.playerUUID = playerUUID;
        this.exited = exited;
    }

    public void setWarLogId(int warLogId) {
        this.warLogId = warLogId;
    }

    @Override
    public int getWarLogId() {
        return warLogId;
    }

    @Override
    public @NotNull String getPlayerName() {
        return playerName;
    }

    @Nullable
    public String getPlayerUUID() {
        return playerUUID;
    }

    public boolean hasExited() {
        return exited;
    }

    public void setExited(boolean exited) {
        this.exited = exited;
    }
}
