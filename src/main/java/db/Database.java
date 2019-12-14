package db;

import db.repository.CommandLogRepository;
import db.repository.TrackChannelRepository;
import db.repository.WorldRepository;
import org.jetbrains.annotations.NotNull;

public interface Database {
    @NotNull
    TrackChannelRepository getTrackingChannelRepository();
    @NotNull
    WorldRepository getWorldRepository();
    @NotNull
    CommandLogRepository getCommandLogRepository();
}
