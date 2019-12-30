package db;

import db.model.commandLog.CommandLog;
import db.repository.CommandLogRepository;
import org.jetbrains.annotations.TestOnly;
import org.junit.jupiter.api.Test;

import java.util.Date;
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
        CommandLog e1 = new CommandLog("up", "up all", null, 1000L, 2000L, new Date());
        CommandLog e2 = new CommandLog("help", "help serverlist", 3000L, 4000L, 5000L, new Date());
        CommandLog e3 = new CommandLog("track", "track server start", 3000L, 6000L, 5000L, new Date());

        assert repo.create(e1);
        assert repo.create(e2);
        assert repo.create(e3);

        assert repo.count() == 3;
        List<CommandLog> logs = repo.findAll();
        assert logs != null;
        assert logs.size() == 3;

        e1 = logs.stream().filter(l -> l.getUserId() == 2000L).findFirst().orElse(null);
        e2 = logs.stream().filter(l -> l.getUserId() == 5000L && l.getChannelId() == 4000L).findFirst().orElse(null);
        assert e1 != null;
        assert e2 != null;

        assert (e2.getId() - 1) == e1.getId();
        assert "up".equals(e1.getKind());
        assert "help serverlist".equals(e2.getFull());
        assert 2000L == e1.getUserId();
        assert e1.getGuildId() == null && e2.getGuildId() != null && e2.getGuildId() == 3000L;

        assert repo.delete(e1);
        assert repo.delete(e2);
        assert repo.count() == 1;
    }
}
