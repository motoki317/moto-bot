package db.model.ignoreChannel;

public class IgnoreChannel implements IgnoreChannelId {
    private long channelId;

    public IgnoreChannel(long channelId) {
        this.channelId = channelId;
    }

    public long getChannelId() {
        return channelId;
    }
}
