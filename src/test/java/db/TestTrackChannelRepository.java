package db;

import db.model.track.TrackChannel;
import db.model.track.TrackType;
import db.repository.base.TrackChannelRepository;
import org.jetbrains.annotations.TestOnly;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

class TestTrackChannelRepository {
    @TestOnly
    private static TrackChannelRepository getRepository() {
        Database db = TestDBUtils.createDatabase();
        return db.getTrackingChannelRepository();
    }

    @TestOnly
    private static void clearTable() {
        TrackChannelRepository repo = getRepository();
        List<TrackChannel> list = repo.findAll();
        assert list != null;
        list.forEach(e -> {
            assert repo.delete(e);
        });
        assert repo.count() == 0;
    }

    @Test
    void testCRUD() {
        clearTable();
        TrackChannelRepository repo = getRepository();

        Date expiresAt = new Date(((System.currentTimeMillis() / 1000) * 1000) + TimeUnit.DAYS.toMillis(30));
        TrackChannel entity = new TrackChannel(TrackType.TERRITORY_ALL, 1000L, 5000L, 10_000L, expiresAt);

        assert repo.count() == 0;
        assert !repo.exists(entity);

        assert repo.create(entity);

        assert repo.count() == 1;
        assert repo.exists(entity);

        List<TrackChannel> res = repo.findAllOfType(TrackType.TERRITORY_ALL);
        assert res != null && res.size() == 1;
        TrackChannel retrieved = res.get(0);
        assert retrieved.getUserId() == 10_000L;
        assert retrieved.getExpiresAt().getTime() == expiresAt.getTime();

        res = repo.findAllOfType(TrackType.WAR_ALL);
        assert res != null && res.size() == 0;

        assert repo.delete(entity);

        assert repo.count() == 0;
        assert !repo.exists(entity);
    }

    @Test
    void testNullableValues() {
        clearTable();
        TrackChannelRepository repo = getRepository();

        long guildId = 2000L;
        long channelId = 6000L;
        long userId = 10_000L;
        Date expiresAt = new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(30));
        TrackChannel entity = new TrackChannel(TrackType.WAR_SPECIFIC, guildId, channelId, userId, expiresAt);
        entity.setGuildName("Salted Test");

        assert repo.create(entity);
        assert repo.exists(entity);

        entity.setGuildName("Different Guild");
        assert !repo.exists(entity);

        entity.setGuildName("Salted Test");
        assert repo.count() == 1;
        assert repo.delete(entity);
        assert repo.count() == 0;
    }
}
