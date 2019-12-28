package api;

import api.structs.OnlinePlayers;
import api.structs.Player;
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

    @Test
    void testGetPlayerStatistics() {
        Player player = getWynnApi().getPlayerStatistics("Salted", false, true);
        assert player != null;
        assert player.getUuid().equals("1ed075fc-5aa9-42e0-a29f-640326c1d80c");
    }
}
