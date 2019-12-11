package db;

import db.model.TrackingChannel;
import db.model.TrackingType;
import db.repository.TrackingChannelRepository;
import org.jetbrains.annotations.TestOnly;
import org.junit.jupiter.api.Test;

class TestTrackingChannelRepository {
    @TestOnly
    private static TrackingChannelRepository getRepository() {
        DatabaseConnection db = TestDBUtils.createConnection();
        return db.getTrackingChannelRepository();
    }

    @Test
    void testCRUD() {
        TrackingChannelRepository repo = getRepository();

        TrackingChannel entity = new TrackingChannel(TrackingType.TERRITORY_ALL, 1000L, 5000L);

        assert repo.count() == 0;
        assert !repo.exists(entity);

        repo.create(entity);
        assert repo.count() == 1;
        assert repo.exists(entity);

        assert repo.findAllOfType(TrackingType.TERRITORY_ALL).size() == 1;
        assert repo.findAllOfType(TrackingType.WAR_ALL).size() == 0;

        repo.delete(entity);
        assert repo.count() == 0;
        assert !repo.exists(entity);
    }
}
