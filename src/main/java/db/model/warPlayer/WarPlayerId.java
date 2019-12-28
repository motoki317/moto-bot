package db.model.warPlayer;

import org.jetbrains.annotations.NotNull;

public interface WarPlayerId {
    int getWarLogId();
    @NotNull
    String getPlayerName();
}
