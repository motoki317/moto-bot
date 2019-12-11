package api;

import api.structs.OnlinePlayers;
import api.structs.TerritoryList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;
import org.junit.jupiter.api.Test;
import utils.TestUtils;

class TestWynnApi {
    @NotNull
    @TestOnly
    private WynnApi getWynnApi() {
        return new WynnApi(TestUtils.getLogger(), TestUtils.getWynnTimeZone());
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
