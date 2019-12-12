package db;

import db.repository.TrackChannelRepository;
import db.repository.WorldRepository;

public interface Database {
    TrackChannelRepository getTrackingChannelRepository();
    WorldRepository getWorldRepository();
}
