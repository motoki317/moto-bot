package db.model.track;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface TrackChannelId {
    @NotNull
    TrackType getType();
    long getGuildId();
    long getChannelId();
    @Nullable
    String getGuildName();
    @Nullable
    String getPlayerName();
}
