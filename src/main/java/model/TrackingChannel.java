package model;

public class TrackingChannel {
    private TrackingType type;
    private long guildId;
    private long channelId;

    public TrackingChannel(TrackingType type, long guildId, long channelId) {
        this.type = type;
        this.guildId = guildId;
        this.channelId = channelId;
    }

    TrackingType getType() {
        return type;
    }

    public long getGuildId() {
        return guildId;
    }

    public long getChannelId() {
        return channelId;
    }
}
