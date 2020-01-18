package db;

import db.model.guildLeaderboard.GuildLeaderboard;
import db.repository.GuildLeaderboardRepository;
import org.jetbrains.annotations.TestOnly;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.Date;
import java.util.List;

class TestGuildLeaderboardRepository {
    @TestOnly
    private static GuildLeaderboardRepository getRepository() {
        Database db = TestDBUtils.createDatabase();
        return db.getGuildLeaderboardRepository();
    }

    @TestOnly
    private static void clearTable() {
        GuildLeaderboardRepository repo = getRepository();
        List<GuildLeaderboard> list = repo.findAll();
        assert list != null;
        list.forEach(e -> {
            assert repo.delete(e);
        });
        assert repo.count() == 0;
    }

    @Test
    void testCRUD() {
        clearTable();
        GuildLeaderboardRepository repo = getRepository();

        Date now = new Date();
        Date old = new Date(now.getTime() - 3600_000L);
        GuildLeaderboard g = new GuildLeaderboard("Kingdom Foxes", "Fox", 30000000000L, 79, 1, 56, 77, old);
        GuildLeaderboard g1 = new GuildLeaderboard("Kingdom Foxes", "Fox", 30609497711L, 79, 1, 56, 77, now);
        GuildLeaderboard g2 = new GuildLeaderboard("Imperial", "Imp", 22008615363L, 75, 2, 51, 77, now);

        assert repo.count() == 0;
        assert !repo.exists(g1);
        assert !repo.exists(g2);

        assert repo.create(g);
        assert repo.create(g1);
        assert repo.create(g2);

        assert repo.count() == 3;
        assert repo.exists(g);
        assert repo.exists(g1);
        assert repo.exists(g2);

        List<GuildLeaderboard> last = repo.getLatestLeaderboard();
        assert last != null;
        assert last.size() == 2;
        last.sort(Comparator.comparingInt(GuildLeaderboard::getNum));
        assert last.get(0).getName().equals("Kingdom Foxes");
        assert last.get(0).getXp() == 30609497711L;

        assert repo.delete(g1);

        assert repo.count() == 2;
    }
}
