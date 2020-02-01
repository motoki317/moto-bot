package db;

import db.model.dateFormat.CustomDateFormat;
import db.model.dateFormat.CustomDateFormatId;
import db.model.dateFormat.CustomFormat;
import db.repository.base.DateFormatRepository;
import org.jetbrains.annotations.TestOnly;
import org.junit.jupiter.api.Test;

import java.util.List;

class TestDateFormatRepository {
    @TestOnly
    private static DateFormatRepository getRepository() {
        Database db = TestDBUtils.createDatabase();
        return db.getDateFormatRepository();
    }

    @TestOnly
    private static void clearTable() {
        DateFormatRepository repo = getRepository();
        List<CustomDateFormat> list = repo.findAll();
        assert list != null;
        list.forEach(e -> {
            assert repo.delete(e);
        });
        assert repo.count() == 0;
    }

    @Test
    void testCRUD() {
        clearTable();
        DateFormatRepository repo = getRepository();

        CustomDateFormat tz1 = new CustomDateFormat(1000L, CustomFormat.TWELVE_HOUR);
        CustomDateFormat tz2 = new CustomDateFormat(5000L, CustomFormat.TWENTY_FOUR_HOUR);

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
        DateFormatRepository repo = getRepository();
        CustomDateFormat tz1 = new CustomDateFormat(10000L, CustomFormat.TWELVE_HOUR);
        CustomDateFormat tz2 = new CustomDateFormat(50000L, CustomFormat.TWENTY_FOUR_HOUR);

        assert !repo.exists(tz1);

        assert repo.create(tz1);
        assert repo.create(tz2);

        CustomDateFormatId tz1id = () -> 10000L;

        assert repo.exists(tz1id);

        CustomDateFormat retrieved = repo.findOne(tz1id);

        assert retrieved != null;
        assert retrieved.getDateFormat().equals(CustomFormat.TWELVE_HOUR);
    }
}
