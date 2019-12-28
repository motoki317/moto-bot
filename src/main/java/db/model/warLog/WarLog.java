package db.model.warLog;

import db.model.warPlayer.WarPlayer;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.List;

public class WarLog implements WarLogId {
    private int id;
    @NotNull
    private String serverName;
    @Nullable
    private String guildName;
    @NotNull
    private Date createdAt;
    @NotNull
    private Date lastUp;
    private boolean ended;
    private boolean logEnded;

    // Join column
    @NotNull
    private List<WarPlayer> players;

    public WarLog(int id, @NotNull String serverName, @Nullable String guildName, @NotNull Date createdAt, @NotNull Date lastUp, boolean ended, boolean logEnded, @NotNull List<WarPlayer> players) {
        this.id = id;
        this.serverName = serverName;
        this.guildName = guildName;
        this.createdAt = createdAt;
        this.lastUp = lastUp;
        this.ended = ended;
        this.logEnded = logEnded;
        this.players = players;
    }

    public WarLog(@NotNull String serverName, @Nullable String guildName, @NotNull Date createdAt, @NotNull Date lastUp, boolean ended, boolean logEnded, @NotNull List<WarPlayer> players) {
        this.serverName = serverName;
        this.guildName = guildName;
        this.createdAt = createdAt;
        this.lastUp = lastUp;
        this.ended = ended;
        this.logEnded = logEnded;
        this.players = players;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public int getId() {
        return id;
    }

    @NotNull
    public String getServerName() {
        return serverName;
    }

    @Nullable
    public String getGuildName() {
        return guildName;
    }

    @NotNull
    public Date getCreatedAt() {
        return createdAt;
    }

    @NotNull
    public Date getLastUp() {
        return lastUp;
    }

    public boolean isEnded() {
        return ended;
    }

    public boolean isLogEnded() {
        return logEnded;
    }

    @NotNull
    public List<WarPlayer> getPlayers() {
        return players;
    }

    public void setLastUp(@NotNull Date lastUp) {
        this.lastUp = lastUp;
    }

    public void setEnded(boolean ended) {
        this.ended = ended;
    }

    public void setLogEnded(boolean logEnded) {
        this.logEnded = logEnded;
    }
}
