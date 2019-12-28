package db.model.warTrack;

public class WarTrack implements WarTrackId {
    private int warLogId;
    private long channelId;
    private long messageId;

    public WarTrack(int warLogId, long channelId, long messageId) {
        this.warLogId = warLogId;
        this.channelId = channelId;
        this.messageId = messageId;
    }

    @Override
    public int getWarLogId() {
        return warLogId;
    }

    @Override
    public long getChannelId() {
        return channelId;
    }

    public long getMessageId() {
        return messageId;
    }
}
