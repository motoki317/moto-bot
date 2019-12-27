package db;

import db.model.territory.Territory;
import db.repository.TerritoryRepository;
import org.jetbrains.annotations.TestOnly;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

class TestTerritoryRepository {
    @TestOnly
    private static TerritoryRepository getRepository() {
        Database db = TestDBUtils.createDatabase();
        return db.getTerritoryRepository();
    }

    @TestOnly
    private static void clearTable() {
        TerritoryRepository repo = getRepository();
        List<Territory> list = repo.findAll();
        assert list != null;
        list.forEach(e -> {
            assert repo.delete(e);
        });
        assert repo.count() == 0;
    }

    @Test
    void testCRUD() {
        clearTable();

        TerritoryRepository repo = getRepository();
        Date now = new Date((System.currentTimeMillis() / 1000) * 1000);
        Territory e1 = new Territory("Ragni", "Salted Test", now, null,
                new Territory.Location(0, 0, 100, 100)
        );
        Territory e2 = new Territory("Detlas", "Grian Test", new Date(now.getTime() - 1000 * 60 * 10), null,
                new Territory.Location(500, 500, 600, 600)
        );

        assert repo.create(e1);
        assert repo.create(e2);

        assert repo.count() == 2;
        List<Territory> territories = repo.findAll();
        assert territories != null;
        assert territories.size() == 2;

        e1 = territories.stream().filter(t -> t.getName().equals("Ragni")).findFirst().orElse(null);
        e2 = territories.stream().filter(t -> t.getName().equals("Detlas")).findFirst().orElse(null);
        assert e1 != null;
        assert e2 != null;

        assert e1.getLocation().getEndX() == 100;
        assert e2.getLocation().getStartX() == 500;
        assert e1.getGuild().equals("Salted Test");
        System.out.println(e1.getAcquired().getTime());
        System.out.println(now.getTime());
        assert e1.getAcquired().getTime() == now.getTime();

        assert repo.delete(e1);
        assert repo.delete(e2);
        assert repo.count() == 0;
    }
}
