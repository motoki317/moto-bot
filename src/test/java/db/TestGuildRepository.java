package db;

import db.model.guild.Guild;
import db.model.guild.GuildId;
import db.repository.GuildRepository;
import org.jetbrains.annotations.TestOnly;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

class TestGuildRepository {
    @TestOnly
    private static GuildRepository getRepository() {
        Database db = TestDBUtils.createDatabase();
        return db.getGuildRepository();
    }

    @TestOnly
    private static void clearTable() {
        GuildRepository repo = getRepository();
        List<Guild> list = repo.findAll();
        assert list != null;
        list.forEach(e -> {
            assert repo.delete(e);
        });
        assert repo.count() == 0;
    }

    @Test
    void testCRUD() {
        clearTable();
        GuildRepository repo = getRepository();

        Guild g1 = new Guild("Kingdom Foxes", "Fox", new Date());
        Guild g2 = new Guild("HackForums", "Hax", new Date());

        assert repo.count() == 0;
        assert !repo.exists(g1);
        assert !repo.exists(g2);

        assert repo.create(g1);
        assert repo.create(g2);

        assert repo.count() == 2;
        assert repo.exists(g1);
        assert repo.exists(g2);

        assert repo.delete(g1);

        assert repo.count() == 1;
    }

    @Test
    void testNonIdFields() {
        clearTable();
        GuildRepository repo = getRepository();
        Guild g1 = new Guild("Kingdom Foxes", "Fox", new Date());
        Guild g2 = new Guild("HackForums", "Hax", new Date());

        assert !repo.exists(g1);

        assert repo.create(g1);
        assert repo.create(g2);

        GuildId p1id = () -> "Kingdom Foxes";

        assert repo.exists(p1id);

        Guild retrieved = repo.findOne(p1id);

        assert retrieved != null;
        assert "Fox".equals(retrieved.getPrefix());
    }
}
