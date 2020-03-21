package db;

import db.model.guildXpLeaderboard.GuildXpLeaderboard;
import db.model.guildXpLeaderboard.GuildXpLeaderboardId;
import db.repository.base.GuildXpLeaderboardRepository;
import org.jetbrains.annotations.TestOnly;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

class TestGuildXpLeaderboardRepository {
    @TestOnly
    private static GuildXpLeaderboardRepository getRepository() {
        Database db = TestDBUtils.createDatabase();
        return db.getGuildXpLeaderboardRepository();
    }

    @TestOnly
    private static void clearTable() {
        GuildXpLeaderboardRepository repo = getRepository();
        List<GuildXpLeaderboard> list = repo.findAll();
        assert list != null;
        list.forEach(e -> {
            assert repo.delete(e);
        });
        assert repo.count() == 0;
    }

    @Test
    void testCRUD() {
        clearTable();
        GuildXpLeaderboardRepository repo = getRepository();

        GuildXpLeaderboard g1 = new GuildXpLeaderboard("Kingdom Foxes", "Fox", 79, 32027567867L, 1_000_000L, new Date(), new Date());
        GuildXpLeaderboard g2 = new GuildXpLeaderboard("Imperial", "Imp", 75, 22384594269L, 500_000L, new Date(), new Date());

        List<GuildXpLeaderboard> list = new ArrayList<>();
        list.add(g1);
        list.add(g2);

        assert repo.count() == 0;
        assert !repo.exists(g1);
        assert !repo.exists(g2);

        assert repo.createAll(list);

        assert repo.count() == 2;
        assert repo.exists(g1);
        assert repo.exists(g2);

        assert repo.getRank("Kingdom Foxes") == 1;
        assert repo.getRank("Imperial") == 2;

        assert repo.delete(g1);

        assert repo.count() == 1;
    }

    @Test
    void testNonIdFields() {
        clearTable();
        GuildXpLeaderboardRepository repo = getRepository();
        GuildXpLeaderboard g1 = new GuildXpLeaderboard("Kingdom Foxes", "Fox", 79, 32027567867L, 1_000_000L, new Date(), new Date());
        GuildXpLeaderboard g2 = new GuildXpLeaderboard("Imperial", "Imp", 75, 22384594269L, 500_000L, new Date(), new Date());

        assert !repo.exists(g1);

        assert repo.create(g1);
        assert repo.create(g2);

        GuildXpLeaderboardId p1id = () -> "Kingdom Foxes";

        assert repo.exists(p1id);

        GuildXpLeaderboard retrieved = repo.findOne(p1id);

        assert retrieved != null;
        assert retrieved.getXpDiff() == 1_000_000L;
    }
}
