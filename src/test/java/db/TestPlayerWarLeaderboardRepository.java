package db;

import db.model.playerWarLeaderboard.PlayerWarLeaderboard;
import db.repository.base.PlayerWarLeaderboardRepository;
import org.jetbrains.annotations.TestOnly;
import org.junit.jupiter.api.Test;

import java.util.List;

class TestPlayerWarLeaderboardRepository {
    @TestOnly
    private static PlayerWarLeaderboardRepository getRepository() {
        Database db = TestDBUtils.createDatabase();
        return db.getPlayerWarLeaderboardRepository();
    }

    @TestOnly
    private static void clearTable() {
        PlayerWarLeaderboardRepository repo = getRepository();
        List<PlayerWarLeaderboard> list = repo.findAll();
        assert list != null;
        list.forEach(e -> {
            assert repo.delete(e);
        });
        assert repo.count() == 0;
    }

    @Test
    void testCRUD() {
        clearTable();
        PlayerWarLeaderboardRepository repo = getRepository();

        PlayerWarLeaderboard p1 = new PlayerWarLeaderboard("ba9aee7f-0585-43cc-b2ae-5d4795ef8265", "Luky0", 1000, 990, 980, null, null);
        PlayerWarLeaderboard p2 = new PlayerWarLeaderboard("c173e626-61d7-4dae-8fcc-9f9809901e76", "Lego_DW", 500, 490, 480, null, null);

        assert repo.count() == 0;
        assert !repo.exists(p1);
        assert !repo.exists(p2);

        assert repo.create(p1);
        assert repo.create(p2);

        assert repo.count() == 2;
        assert repo.exists(p1);
        assert repo.exists(p2);

        List<PlayerWarLeaderboard> last = repo.findAll();
        assert last != null;
        assert last.size() == 2;

        last.sort((c1, c2) -> Double.compare(c2.getSuccessRate(), c1.getSuccessRate()));
        assert last.get(0).getLastName().equals("Luky0");

        last.sort((c1, c2) -> Double.compare(c2.getSurvivedRate(), c1.getSurvivedRate()));
        assert last.get(0).getLastName().equals("Luky0");

        assert repo.delete(p1);

        assert repo.count() == 1;
    }
}
