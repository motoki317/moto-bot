package db.model.track;

import org.jetbrains.annotations.NotNull;

public interface TrackChannelId {
    @NotNull
    TrackType getType();
    long getGuildId();
    long getChannelId();
}
