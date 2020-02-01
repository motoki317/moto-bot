package db;

import db.model.timezone.CustomTimeZone;
import db.model.timezone.CustomTimeZoneId;
import db.repository.base.TimeZoneRepository;
import org.jetbrains.annotations.TestOnly;
import org.junit.jupiter.api.Test;

import java.util.List;

class TestTimeZoneRepository {
    @TestOnly
    private static TimeZoneRepository getRepository() {
        Database db = TestDBUtils.createDatabase();
        return db.getTimeZoneRepository();
    }

    @TestOnly
    private static void clearTable() {
        TimeZoneRepository repo = getRepository();
        List<CustomTimeZone> list = repo.findAll();
        assert list != null;
        list.forEach(e -> {
            assert repo.delete(e);
        });
        assert repo.count() == 0;
    }

    @Test
    void testCRUD() {
        clearTable();
        TimeZoneRepository repo = getRepository();

        CustomTimeZone tz1 = new CustomTimeZone(1000L, "UTC");
        CustomTimeZone tz2 = new CustomTimeZone(5000L, "+0200");

        assert repo.count() == 0;
        assert !repo.exists(tz1);
        assert !repo.exists(tz2);

        assert repo.create(tz1);
        assert repo.create(tz2);

        assert repo.count() == 2;
        assert repo.exists(tz1);
        assert repo.exists(tz2);

        assert repo.delete(tz1);

        assert repo.count() == 1;
    }

    @Test
    void testNonIdFields() {
        clearTable();
        TimeZoneRepository repo = getRepository();
        CustomTimeZone tz1 = new CustomTimeZone(10000L, "+0900");
        CustomTimeZone tz2 = new CustomTimeZone(50000L, "JST");

        assert !repo.exists(tz1);

        assert repo.create(tz1);
        assert repo.create(tz2);

        CustomTimeZoneId tz1id = () -> 10000L;

        assert repo.exists(tz1id);

        CustomTimeZone retrieved = repo.findOne(tz1id);

        assert retrieved != null;
        assert "+0900".equals(retrieved.getTimezone());
    }
}
