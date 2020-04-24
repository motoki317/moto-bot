package db.model.playerNumber;

import org.jetbrains.annotations.NotNull;

import java.util.Date;

public class PlayerNumber implements PlayerNumberId {
    @NotNull
    private final Date dateTime;
    private final int playerNum;

    public PlayerNumber(@NotNull Date dateTime, int playerNum) {
        this.dateTime = dateTime;
        this.playerNum = playerNum;
    }

    @NotNull
    public Date getDateTime() {
        return dateTime;
    }

    public int getPlayerNum() {
        return playerNum;
    }
}
