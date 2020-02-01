package db;

import db.model.ignoreChannel.IgnoreChannel;
import db.repository.base.IgnoreChannelRepository;
import org.jetbrains.annotations.TestOnly;
import org.junit.jupiter.api.Test;

import java.util.List;

class TestIgnoreChannelRepository {
    @TestOnly
    private static IgnoreChannelRepository getRepository() {
        Database db = TestDBUtils.createDatabase();
        return db.getIgnoreChannelRepository();
    }

    @TestOnly
    private static void clearTable() {
        IgnoreChannelRepository repo = getRepository();
        List<IgnoreChannel> list = repo.findAll();
        assert list != null;
        list.forEach(e -> {
            assert repo.delete(e);
        });
        assert repo.count() == 0;
    }

    @Test
    void testCRUD() {
        clearTable();
        IgnoreChannelRepository repo = getRepository();

        IgnoreChannel p1 = new IgnoreChannel(1000L);
        IgnoreChannel p2 = new IgnoreChannel(5000L);

        assert repo.count() == 0;
        assert !repo.exists(p1);
        assert !repo.exists(p2);

        assert repo.create(p1);
        assert repo.create(p2);

        assert repo.count() == 2;
        assert repo.exists(p1);
        assert repo.exists(p2);
        assert !repo.exists(() -> 6000L);

        assert repo.delete(p1);

        assert repo.count() == 1;
    }
}
