package db;

import db.repository.TrackingChannelRepository;

public interface Database {
    TrackingChannelRepository getTrackingChannelRepository();
}
