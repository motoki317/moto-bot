package db.model.track;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TrackChannel implements TrackChannelId {
    @NotNull
    private TrackType type;
    private long guildId;
    private long channelId;

    @Nullable
    private String guildName;
    @Nullable
    private String playerName;

    public TrackChannel(@NotNull TrackType type, long guildId, long channelId) {
        this.type = type;
        this.guildId = guildId;
        this.channelId = channelId;
    }

    @NotNull
    public TrackType getType() {
        return type;
    }

    public long getGuildId() {
        return guildId;
    }

    public long getChannelId() {
        return channelId;
    }

    @Nullable
    public String getGuildName() {
        return guildName;
    }

    public void setGuildName(@Nullable String guildName) {
        this.guildName = guildName;
    }

    @Nullable
    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(@Nullable String playerName) {
        this.playerName = playerName;
    }

    @NotNull
    public String getDisplayName() {
        String ret = this.type.getDisplayName();
        switch (this.type) {
            case WAR_SPECIFIC:
            case TERRITORY_SPECIFIC:
                ret += " (Guild: " + this.guildName + ")";
                break;
            case WAR_PLAYER:
                ret += " (Player: " + this.playerName + ")";
                break;
        }
        return ret;
    }
}
