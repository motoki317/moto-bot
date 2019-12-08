package api;

import api.structs.OnlinePlayers;
import api.structs.TerritoryList;
import log.ConsoleLogger;
import org.junit.jupiter.api.Test;

import java.util.TimeZone;

class TestWynnApi {
    @Test
    void testGetOnlinePlayers() {
        WynnApi api = new WynnApi(new ConsoleLogger(TimeZone.getTimeZone("Asia/Tokyo")));
        OnlinePlayers players = api.getOnlinePlayers();
        assert players != null;
    }

    @Test
    void testGetTerritoryList() {
        WynnApi api = new WynnApi(new ConsoleLogger(TimeZone.getTimeZone("Asia/Tokyo")));
        TerritoryList territoryList = api.getTerritoryList();
        assert territoryList != null;
    }
}
