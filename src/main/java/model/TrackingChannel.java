package model;

public class TrackingChannel {
    private long guildId;
    private long channelId;

    public TrackingChannel(long guildId, long channelId) {
        this.guildId = guildId;
        this.channelId = channelId;
    }

    public long getGuildId() {
        return guildId;
    }

    public long getChannelId() {
        return channelId;
    }
}
