package db.model.musicQueue;

import java.util.Date;

public class MusicQueueEntry implements MusicQueueEntryId {
    private long guildId;
    private int index;
    private long userId;
    private String url;
    private long position;
    private Date updatedAt;

    public MusicQueueEntry(long guildId, int index, long userId, String url, long position, Date updatedAt) {
        this.guildId = guildId;
        this.index = index;
        this.userId = userId;
        this.url = url;
        this.position = position;
        this.updatedAt = updatedAt;
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

    public Date getUpdatedAt() {
        return updatedAt;
    }
}
