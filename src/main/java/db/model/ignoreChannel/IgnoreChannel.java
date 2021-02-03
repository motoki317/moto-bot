package db.model.ignoreChannel;

public class IgnoreChannel implements IgnoreChannelId {
    private final long channelId;

    public IgnoreChannel(long channelId) {
        this.channelId = channelId;
    }

    public long getChannelId() {
        return channelId;
    }
}
