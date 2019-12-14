package db;

import db.model.commandLog.CommandLog;
import db.repository.CommandLogRepository;
import org.jetbrains.annotations.TestOnly;
import org.junit.jupiter.api.Test;

import java.util.List;

class TestCommandLogRepository {
    @TestOnly
    private static CommandLogRepository getRepository() {
        Database db = TestDBUtils.createDatabase();
        return db.getCommandLogRepository();
    }

    @TestOnly
    private static void clearTable() {
        CommandLogRepository repo = getRepository();
        List<CommandLog> list = repo.findAll();
        assert list != null;
        list.forEach(e -> {
            assert repo.delete(e);
        });
        assert repo.count() == 0;
    }

    @Test
    void testCRUD() {
        clearTable();

        CommandLogRepository repo = getRepository();
        CommandLog e1 = new CommandLog("up", "up all", 1234L, true);
        CommandLog e2 = new CommandLog("help", "help serverlist", 5678L, false);

        assert repo.create(e1);
        assert repo.create(e2);

        assert repo.count() == 2;
        List<CommandLog> logs = repo.findAll();
        assert logs != null;
        assert logs.size() == 2;

        e1 = logs.stream().filter(l -> l.getUserId() == 1234L).findFirst().orElse(null);
        e2 = logs.stream().filter(l -> l.getUserId() == 5678L).findFirst().orElse(null);
        assert e1 != null;
        assert e2 != null;

        assert (e2.getId() - 1) == e1.getId();
        assert "up".equals(e1.getKind());
        assert "help serverlist".equals(e2.getFull());
        assert 1234L == e1.getUserId();
        assert e1.isDm() && !e2.isDm();

        assert repo.delete(e1);
        assert repo.delete(e2);
        assert repo.count() == 0;
    }
}
