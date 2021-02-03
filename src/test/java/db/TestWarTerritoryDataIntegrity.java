package db;

import db.model.guildWarLog.GuildWarLog;
import db.model.territory.Territory;
import db.model.territoryLog.TerritoryLog;
import db.model.warLog.WarLog;
import db.model.warPlayer.WarPlayer;
import db.repository.base.GuildWarLogRepository;
import db.repository.base.TerritoryLogRepository;
import db.repository.base.TerritoryRepository;
import db.repository.base.WarLogRepository;
import org.jetbrains.annotations.TestOnly;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("OverlyLongMethod")
class TestWarTerritoryDataIntegrity {
    private static final DateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    @TestOnly
    private static List<Territory> prepareTerritories() {
        List<Territory> ret = new ArrayList<>();
        try {
            ret.add(
                    new Territory(
                            "Detlas",
                            "Kingdom Foxes",
                            formatter.parse("2020/01/01 15:00:00"),
                            null,
                            new Territory.Location(100, 100, 200, 200)
                    )
            );
            ret.add(
                    new Territory(
                            "Ragni",
                            "HackForums",
                            formatter.parse("2020/01/01 15:00:00"),
                            null,
                            new Territory.Location(500, 500, 600, 600)
                    )
            );
        } catch (ParseException e) {
            e.printStackTrace();
            assert false;
        }
        return ret;
    }

    @TestOnly
    private static void clearTables() {
        ConnectionPool pool = TestDBUtils.createConnection();
        Connection conn = pool.getConnection();
        assert conn != null;
        try {
            Statement statement = conn.createStatement();
            statement.executeQuery("TRUNCATE TABLE `territory`");
            statement.executeQuery("TRUNCATE TABLE `guild_war_log`");
            // to avoid foreign key constraint
            statement.executeQuery("DELETE FROM `war_log` WHERE 1 = 1");
            statement.executeQuery("DELETE FROM `territory_log` WHERE 1 = 1");
        } catch (SQLException e) {
            e.printStackTrace();
            assert false;
        }
    }

    @Test
    void testTerritoryLog() throws ParseException {
        clearTables();
        Database db = TestDBUtils.createDatabase();

        TerritoryRepository territoryRepository = db.getTerritoryRepository();
        TerritoryLogRepository territoryLogRepository = db.getTerritoryLogRepository();
        GuildWarLogRepository guildWarLogRepository = db.getGuildWarLogRepository();

        assert territoryRepository.count() == 0;
        assert territoryLogRepository.count() == 0;
        assert guildWarLogRepository.count() == 0;

        List<Territory> territories = prepareTerritories();
        assert territoryRepository.updateAll(territories);

        // actual test starts here

        Date acquired = formatter.parse("2020/01/01 15:30:00");
        territories.set(0, new Territory(
                "Detlas",
                "HackForums",
                acquired,
                null,
                new Territory.Location(100, 100, 200, 200)
        ));
        assert territoryRepository.updateAll(territories);

        assert territoryLogRepository.count() == 1;

        int territoryLogLastId = territoryLogRepository.lastInsertId();
        assert territoryLogLastId != -1;
        TerritoryLog territoryLog = territoryLogRepository.findOne(() -> territoryLogLastId);
        assert territoryLog != null;

        assert "Detlas".equals(territoryLog.getTerritoryName());
        assert "Kingdom Foxes".equals(territoryLog.getOldGuildName());
        assert "HackForums".equals(territoryLog.getNewGuildName());
        assert territoryLog.getOldGuildTerrAmt() == 0;
        assert territoryLog.getNewGuildTerrAmt() == 2;
        assert territoryLog.getAcquired().getTime() == acquired.getTime();
        assert territoryLog.getTimeDiff() == TimeUnit.MINUTES.toMillis(30);

        assert guildWarLogRepository.count() == 2;
        GuildWarLog wonLog = Objects.requireNonNull(guildWarLogRepository.findGuildLogs("HackForums", 1, 0)).get(0);
        GuildWarLog lostLog = Objects.requireNonNull(guildWarLogRepository.findGuildLogs("Kingdom Foxes", 1, 0)).get(0);

        assert wonLog != null && lostLog != null;
        assert wonLog.getTerritoryLogId() != null && wonLog.getTerritoryLogId() == territoryLogLastId;
        assert lostLog.getTerritoryLogId() != null && lostLog.getTerritoryLogId() == territoryLogLastId;
        assert wonLog.getWarLogId() == null;
        assert lostLog.getWarLogId() == null;
    }

    @Test
    void testWithWar() throws ParseException {
        clearTables();
        Database db = TestDBUtils.createDatabase();

        TerritoryRepository territoryRepository = db.getTerritoryRepository();
        TerritoryLogRepository territoryLogRepository = db.getTerritoryLogRepository();
        WarLogRepository warLogRepository = db.getWarLogRepository();
        GuildWarLogRepository guildWarLogRepository = db.getGuildWarLogRepository();

        assert territoryRepository.count() == 0;
        assert warLogRepository.count() == 0;
        assert territoryLogRepository.count() == 0;
        assert guildWarLogRepository.count() == 0;

        List<Territory> territories = prepareTerritories();
        assert territoryRepository.updateAll(territories);

        // actual test starts here

        Date warStart = formatter.parse("2020/01/01 15:27:00");
        List<WarPlayer> warPlayers = new ArrayList<>();
        warPlayers.add(new WarPlayer("TheDarkSoul", null, false));
        warPlayers.add(new WarPlayer("BisexualDog", null, false));
        WarLog warLog = new WarLog("WAR123", "HackForums", warStart, warStart, false, false, warPlayers);
        int warLogId = warLogRepository.createAndGetLastInsertId(warLog);
        assert warLogId != 0;

        warLog = warLogRepository.findOne(() -> warLogId);
        assert warLog != null;
        warLog.setLastUp(formatter.parse("2020/01/01 15:29:00"));
        assert warLogRepository.update(warLog);

        warLog = warLogRepository.findOne(() -> warLogId);
        assert warLog != null;
        warLog.setLastUp(formatter.parse("2020/01/01 15:29:30"));
        warLog.setEnded(true);
        warLog.setLogEnded(true);
        assert warLogRepository.update(warLog);

        Date acquired = formatter.parse("2020/01/01 15:30:00");
        territories.set(0, new Territory(
                "Detlas",
                "HackForums",
                acquired,
                null,
                new Territory.Location(100, 100, 200, 200)
        ));
        assert territoryRepository.updateAll(territories);

        assert warLogRepository.count() == 1;
        assert territoryLogRepository.count() == 1;
        assert guildWarLogRepository.count() == 2;

        int territoryLogLastId = territoryLogRepository.lastInsertId();
        assert territoryLogLastId != -1;

        GuildWarLog wonLog = Objects.requireNonNull(guildWarLogRepository.findGuildLogs("HackForums", 1, 0)).get(0);
        GuildWarLog lostLog = Objects.requireNonNull(guildWarLogRepository.findGuildLogs("Kingdom Foxes", 1, 0)).get(0);

        assert wonLog != null && lostLog != null;
        assert wonLog.getTerritoryLogId() != null && wonLog.getTerritoryLogId() == territoryLogLastId;
        assert lostLog.getTerritoryLogId() != null && lostLog.getTerritoryLogId() == territoryLogLastId;
        assert wonLog.getWarLogId() != null && wonLog.getWarLogId() == warLogId;
        assert lostLog.getWarLogId() == null;
    }

    @Test
    void testTerritoryTakenFirstWithWar() throws ParseException {
        clearTables();
        Database db = TestDBUtils.createDatabase();

        TerritoryRepository territoryRepository = db.getTerritoryRepository();
        TerritoryLogRepository territoryLogRepository = db.getTerritoryLogRepository();
        WarLogRepository warLogRepository = db.getWarLogRepository();
        GuildWarLogRepository guildWarLogRepository = db.getGuildWarLogRepository();

        assert territoryRepository.count() == 0;
        assert warLogRepository.count() == 0;
        assert territoryLogRepository.count() == 0;
        assert guildWarLogRepository.count() == 0;

        List<Territory> territories = prepareTerritories();
        assert territoryRepository.updateAll(territories);

        // actual test starts here

        Date warStart = formatter.parse("2020/01/01 15:27:00");
        List<WarPlayer> warPlayers = new ArrayList<>();
        warPlayers.add(new WarPlayer("TheDarkSoul", null, false));
        warPlayers.add(new WarPlayer("BisexualDog", null, false));
        WarLog warLog = new WarLog("WAR123", "HackForums", warStart, warStart, false, false, warPlayers);
        int warLogId = warLogRepository.createAndGetLastInsertId(warLog);
        assert warLogId != 0;

        warLog = warLogRepository.findOne(() -> warLogId);
        assert warLog != null;
        warLog.setLastUp(formatter.parse("2020/01/01 15:29:00"));
        assert warLogRepository.update(warLog);

        warLog = warLogRepository.findOne(() -> warLogId);
        assert warLog != null;
        warLog.setLastUp(formatter.parse("2020/01/01 15:29:30"));
        assert warLogRepository.update(warLog);

        Date acquired = formatter.parse("2020/01/01 15:30:00");
        territories.set(0, new Territory(
                "Detlas",
                "HackForums",
                acquired,
                null,
                new Territory.Location(100, 100, 200, 200)
        ));
        assert territoryRepository.updateAll(territories);

        warLog = warLogRepository.findOne(() -> warLogId);
        assert warLog != null;
        assert warLog.isEnded();
        assert !warLog.isLogEnded();
        warLog.setLogEnded(true);
        assert warLogRepository.update(warLog);

        assert warLogRepository.count() == 1;
        assert territoryLogRepository.count() == 1;
        assert guildWarLogRepository.count() == 2;

        int territoryLogLastId = territoryLogRepository.lastInsertId();
        assert territoryLogLastId != -1;

        GuildWarLog wonLog = Objects.requireNonNull(guildWarLogRepository.findGuildLogs("HackForums", 1, 0)).get(0);
        GuildWarLog lostLog = Objects.requireNonNull(guildWarLogRepository.findGuildLogs("Kingdom Foxes", 1, 0)).get(0);

        assert wonLog != null && lostLog != null;
        assert wonLog.getTerritoryLogId() != null && wonLog.getTerritoryLogId() == territoryLogLastId;
        assert lostLog.getTerritoryLogId() != null && lostLog.getTerritoryLogId() == territoryLogLastId;
        assert wonLog.getWarLogId() != null && wonLog.getWarLogId() == warLogId;
        assert lostLog.getWarLogId() == null;
    }
}
