package db.model.track;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;
import java.util.Objects;

public class TrackChannel implements TrackChannelId {
    @NotNull
    private TrackType type;
    private long guildId;
    private long channelId;

    @Nullable
    private String guildName;
    @Nullable
    private String playerName;

    private long userId;
    @NotNull
    private Date expiresAt;

    public TrackChannel(@NotNull TrackType type, long guildId, long channelId, long userId, @NotNull Date expiresAt) {
        this.type = type;
        this.guildId = guildId;
        this.channelId = channelId;
        this.userId = userId;
        this.expiresAt = expiresAt;
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

    public long getUserId() {
        return userId;
    }

    @NotNull
    public Date getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(@NotNull Date expiresAt) {
        this.expiresAt = expiresAt;
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

    @Override
    public String toString() {
        return this.getDisplayName() + " guild id: " + guildId + " channel id: " + channelId;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof TrackChannel
                && ((TrackChannel) obj).type == this.type
                && ((TrackChannel) obj).guildId == this.guildId
                && ((TrackChannel) obj).channelId == this.channelId
                && ((((TrackChannel) obj).guildName == null && this.guildName == null) || (this.guildName != null && this.guildName.equals(((TrackChannel) obj).guildName)))
                && ((((TrackChannel) obj).playerName == null && this.playerName == null) || (this.playerName != null && this.playerName.equals(((TrackChannel) obj).playerName)));
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.type, this.guildId, this.channelId, this.guildName, this.playerName);
    }
}
