package db.model.musicInterruptedGuild;

public class MusicInterruptedGuild implements MusicInterruptedGuildId {
    private final long guildId;
    private final long channelId;
    private final long voiceChannelId;

    public MusicInterruptedGuild(long guildId, long channelId, long voiceChannelId) {
        this.guildId = guildId;
        this.channelId = channelId;
        this.voiceChannelId = voiceChannelId;
    }

    @Override
    public long getGuildId() {
        return guildId;
    }

    public long getChannelId() {
        return channelId;
    }

    public long getVoiceChannelId() {
        return voiceChannelId;
    }
}
