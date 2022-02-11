package db;

import db.model.guild.Guild;
import db.model.guild.GuildId;
import db.repository.base.GuildRepository;
import org.jetbrains.annotations.TestOnly;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

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

    @Test
    void testCaseSensitivity() {
        clearTable();
        GuildRepository repo = getRepository();
        Guild g1 = new Guild("Kingdom Foxes", "Fox", new Date());
        Guild g2 = new Guild("Kingdom foxes", "fox", new Date());

        assert !repo.exists(g1);

        assert repo.create(g1);
        assert repo.create(g2);

        assert repo.exists(g1);
        assert repo.exists(g2);

        assert repo.count() == 2;

        Guild csSearch = repo.findOne(() -> "Kingdom foxes");
        assert csSearch != null && "fox".equals(csSearch.getPrefix());

        List<Guild> ciSearch = repo.findAllCaseInsensitive("Kingdom Foxes");
        assert ciSearch != null && ciSearch.size() == 2;
    }

    @Test
    void testTrailingSpaces() {
        clearTable();
        GuildRepository repo = getRepository();
        Guild g1 = new Guild("Kingdom Foxes", "Fox", new Date());
        Guild g2 = new Guild("Kingdom Foxes ", "fox", new Date());

        assert !repo.exists(g1);

        assert repo.create(g1);
        assert repo.create(g2);

        assert repo.exists(g1);
        assert repo.exists(g2);

        assert repo.count() == 2;

        Guild csSearch = repo.findOne(() -> "Kingdom Foxes ");
        assert csSearch != null && "fox".equals(csSearch.getPrefix());

        List<Guild> ciSearch = repo.findAllCaseInsensitive("Kingdom Foxes");
        assert ciSearch != null && ciSearch.size() == 2;
    }

    @Test
    void testPrefixCI() {
        clearTable();
        GuildRepository repo = getRepository();
        Guild g1 = new Guild("FoxTaleSkeletons", "FOX", new Date());
        Guild g2 = new Guild("Kingdom Foxes", "Fox", new Date());
        Guild g3 = new Guild("illuminati", "fox", new Date());
        Guild g4 = new Guild("HackForums", "Hax", new Date());

        assert repo.create(g1) && repo.create(g2) && repo.create(g3) && repo.create(g4);
        assert repo.count() == 4;

        List<Guild> retrieved = repo.findAllByPrefixCaseInsensitive("Fox");
        assert retrieved != null && retrieved.size() == 3;
        List<String> names = retrieved.stream().map(Guild::getName).toList();
        assert names.contains("FoxTaleSkeletons");
        assert names.contains("Kingdom Foxes");
        assert names.contains("illuminati");
        assert !names.contains("HackForums");
    }

    @Test
    void testPrefixCS() {
        clearTable();
        GuildRepository repo = getRepository();
        Guild g1 = new Guild("FoxTaleSkeletons", "FOX", new Date());
        Guild g2 = new Guild("Kingdom Foxes", "Fox", new Date());
        Guild g3 = new Guild("illuminati", "fox", new Date());
        Guild g4 = new Guild("HackForums", "Hax", new Date());

        assert repo.create(g1) && repo.create(g2) && repo.create(g3) && repo.create(g4);
        assert repo.count() == 4;

        List<Guild> retrieved = repo.findAllByPrefix("Fox");
        assert retrieved != null && retrieved.size() == 1;
        List<String> names = retrieved.stream().map(Guild::getName).toList();
        assert !names.contains("FoxTaleSkeletons");
        assert names.contains("Kingdom Foxes");
        assert !names.contains("illuminati");
        assert !names.contains("HackForums");
    }
}
