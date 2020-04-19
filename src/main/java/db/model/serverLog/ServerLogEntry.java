package db.model.serverLog;

public class ServerLogEntry implements ServerLogEntryId {
    private long guildId;
    private long channelId;

    public ServerLogEntry(long guildId, long channelId) {
        this.guildId = guildId;
        this.channelId = channelId;
    }

    @Override
    public long getGuildId() {
        return guildId;
    }

    public long getChannelId() {
        return channelId;
    }
}
