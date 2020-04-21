package db.model.musicQueue;

public class MusicQueueEntry implements MusicQueueEntryId {
    private long guildId;
    private int index;
    private long userId;
    private String url;
    private long position;

    public MusicQueueEntry(long guildId, int index, long userId, String url, long position) {
        this.guildId = guildId;
        this.index = index;
        this.userId = userId;
        this.url = url;
        this.position = position;
    }

    @Override
    public long getGuildId() {
        return guildId;
    }

    @Override
    public int getIndex() {
        return index;
    }

    public long getUserId() {
        return userId;
    }

    public String getUrl() {
        return url;
    }

    public long getPosition() {
        return position;
    }
}
