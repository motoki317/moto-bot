package api;

import api.structs.OnlinePlayers;
import api.structs.TerritoryList;
import log.ConsoleLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;
import org.junit.jupiter.api.Test;

import java.util.TimeZone;

class TestWynnApi {
    @NotNull
    @TestOnly
    private WynnApi getWynnApi() {
        TimeZone log = TimeZone.getTimeZone("Asia/Tokyo");
        TimeZone wynn = TimeZone.getTimeZone("America/New_York");
        return new WynnApi(new ConsoleLogger(log), wynn);
    }

    @Test
    void testGetOnlinePlayers() {
        OnlinePlayers players = getWynnApi().getOnlinePlayers();
        assert players != null;
        assert players.getWorlds().entrySet().size() > 0;
    }

    @Test
    void testGetTerritoryList() {
        TerritoryList territoryList = getWynnApi().getTerritoryList();
        assert territoryList != null;
        assert territoryList.getTerritories().entrySet().size() > 300;
    }
}
