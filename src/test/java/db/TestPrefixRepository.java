package db;

import db.model.prefix.Prefix;
import db.model.prefix.PrefixId;
import db.repository.base.PrefixRepository;
import org.jetbrains.annotations.TestOnly;
import org.junit.jupiter.api.Test;

import java.util.List;

class TestPrefixRepository {
    @TestOnly
    private static PrefixRepository getRepository() {
        Database db = TestDBUtils.createDatabase();
        return db.getPrefixRepository();
    }

    @TestOnly
    private static void clearTable() {
        PrefixRepository repo = getRepository();
        List<Prefix> list = repo.findAll();
        assert list != null;
        list.forEach(e -> {
            assert repo.delete(e);
        });
        assert repo.count() == 0;
    }

    @Test
    void testCRUD() {
        clearTable();
        PrefixRepository repo = getRepository();

        Prefix p1 = new Prefix(1000L, ".");
        Prefix p2 = new Prefix(5000L, "/");

        assert repo.count() == 0;
        assert !repo.exists(p1);
        assert !repo.exists(p2);

        assert repo.create(p1);
        assert repo.create(p2);

        assert repo.count() == 2;
        assert repo.exists(p1);
        assert repo.exists(p2);

        assert repo.delete(p1);

        assert repo.count() == 1;
    }

    @Test
    void testNonIdFields() {
        clearTable();
        PrefixRepository repo = getRepository();
        Prefix p1 = new Prefix(10000L, ",");
        Prefix p2 = new Prefix(50000L, ";;");

        assert !repo.exists(p1);

        assert repo.create(p1);
        assert repo.create(p2);

        PrefixId p1id = () -> 10000L;

        assert repo.exists(p1id);

        Prefix retrieved = repo.findOne(p1id);

        assert retrieved != null;
        assert ",".equals(retrieved.getPrefix());
    }
}
