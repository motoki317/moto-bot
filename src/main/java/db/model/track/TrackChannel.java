package db.model.track;

import org.jetbrains.annotations.NotNull;

public class TrackChannel implements TrackChannelId {
    // Below 3 fields identify a track channel entry
    private TrackType type;
    private long guildId;
    private long channelId;

    private String guildName;
    private String playerName;

    public TrackChannel(TrackType type, long guildId, long channelId) {
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

    public String getGuildName() {
        return guildName;
    }

    public void setGuildName(String guildName) {
        this.guildName = guildName;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }
}
