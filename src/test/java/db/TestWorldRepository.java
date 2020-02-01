package db;

import db.model.world.World;
import db.model.world.WorldId;
import db.repository.base.WorldRepository;
import org.jetbrains.annotations.TestOnly;
import org.junit.jupiter.api.Test;

import java.util.List;

class TestWorldRepository {
    @TestOnly
    private static WorldRepository getRepository() {
        Database db = TestDBUtils.createDatabase();
        return db.getWorldRepository();
    }

    @TestOnly
    private static void clearTable() {
        WorldRepository repo = getRepository();
        List<World> list = repo.findAll();
        assert list != null;
        list.forEach(e -> {
            assert repo.delete(e);
        });
        assert repo.count() == 0;
    }

    @Test
    void testCRUD() {
        clearTable();
        WorldRepository repo = getRepository();

        World wc1 = new World("WC1", 30);
        World lobby1 = new World("lobby1", 10);

        assert repo.count() == 0;
        assert !repo.exists(wc1);
        assert !repo.exists(lobby1);

        assert repo.create(wc1);
        assert repo.create(lobby1);

        assert repo.count() == 2;
        assert repo.exists(wc1);
        assert repo.exists(lobby1);

        List<World> mainWorlds = repo.findAllMainWorlds();
        assert mainWorlds != null;
        assert mainWorlds.size() == 1;
        List<World> all = repo.findAll();
        assert all != null && all.size() == 2;

        assert repo.delete(wc1);

        assert repo.count() == 1;
        mainWorlds = repo.findAllMainWorlds();
        assert mainWorlds != null;
        assert mainWorlds.size() == 0;
    }

    @Test
    void testNonIdFields() {
        clearTable();
        WorldRepository repo = getRepository();
        World wc5 = new World("WC5", 50);

        assert !repo.exists(wc5);

        assert repo.create(wc5);

        WorldId wc5id = () -> "WC5";

        assert repo.exists(wc5id);

        World retrieved = repo.findOne(wc5id);

        assert retrieved != null;
        assert "WC5".equals(retrieved.getName());
        assert retrieved.getPlayers() == 50;
        assert retrieved.getCreatedAt() != null;
        assert retrieved.getUpdatedAt() != null;
    }
}
