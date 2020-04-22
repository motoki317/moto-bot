package db.model.musicInterruptedGuild;

public class MusicInterruptedGuild implements MusicInterruptedGuildId {
    private final long guildId;
    private final long channelId;

    public MusicInterruptedGuild(long guildId, long channelId) {
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
