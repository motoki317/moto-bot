package db;

import db.repository.TrackChannelRepository;

public interface Database {
    TrackChannelRepository getTrackingChannelRepository();
}
