package api;

import api.wynn.WynnApi;
import api.wynn.structs.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;
import org.junit.jupiter.api.Test;
import utils.TestUtils;

class TestWynnApi {
    @NotNull
    @TestOnly
    private WynnApi getWynnApi() {
        return new WynnApi(TestUtils.getLogger());
    }

    @Test
    void testGetOnlinePlayers() {
        OnlinePlayers players = getWynnApi().mustGetOnlinePlayers();
        assert players != null;
        assert players.getWorlds().entrySet().size() > 0;
    }

    @Test
    void testGetTerritoryList() {
        TerritoryList territoryList = getWynnApi().mustGetTerritoryList();
        assert territoryList != null;
        assert territoryList.getTerritories().entrySet().size() > 300;
    }

    @Test
    void testGetPlayerStatistics() {
        Player player = getWynnApi().mustGetPlayerStatistics("Salted", true);
        assert player != null;
        assert player.getUuid().equals("1ed075fc-5aa9-42e0-a29f-640326c1d80c");
    }

    @Test
    void testGuildList() {
        GuildList guildList = getWynnApi().mustGetGuildList();
        assert guildList != null;
        assert guildList.getGuilds().size() > 1000;
    }

    @Test
    void testGuildStats() {
        WynnGuild guild = getWynnApi().mustGetGuildStats("HackForums");
        assert guild != null;
        assert "Hax".equals(guild.getPrefix());
    }

    @Test
    void testGuildLeaderboard() {
        WynnGuildLeaderboard leaderboard = getWynnApi().mustGetGuildLeaderboard();
        assert leaderboard != null;
        assert leaderboard.getData().size() == 100;
    }

    @Test
    void testItemDB() {
        ItemDB items = getWynnApi().mustGetItemDB(true);
        assert items != null;
        assert items.getItems().size() > 1000;
    }
}
