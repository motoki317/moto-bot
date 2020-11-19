package api;

import api.mojang.MojangApi;
import api.mojang.structs.NameHistory;
import api.mojang.structs.NullableUUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;
import org.junit.jupiter.api.Test;
import utils.TestUtils;
import utils.UUID;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

class TestMojangApi {
    @NotNull
    @TestOnly
    MojangApi getApi() {
        return new MojangApi(TestUtils.getLogger());
    }

    @Test
    void testNamesToUUIDs() {
        MojangApi api = getApi();
        Map<String, String> playerList = new HashMap<>();
        playerList.put("Salted", "1ed075fc-5aa9-42e0-a29f-640326c1d80c");
        Map<String, NullableUUID> res = api.getUUIDsIterative(new ArrayList<>(playerList.keySet()));
        assert res != null;
        for (String player : playerList.keySet()) {
            assert res.containsKey(player);
            UUID uuid = res.get(player).getUuid();
            assert uuid != null;
            assert uuid.toStringWithHyphens().equals(playerList.get(player));
        }
    }

    @Test
    void testUUIDAtTime() {
        MojangApi api = getApi();
        UUID uuid = api.mustGetUUIDAtTime("d3keract", 1605752036000L);
        assert uuid != null;
        assert uuid.toStringWithHyphens().equals("db9b5cad-5785-48d3-be91-18a9876ec00e");
    }

    @Test
    void testNameHistory() {
        MojangApi api = getApi();
        NameHistory history = api.mustGetNameHistory(new UUID("1d378fba-e8d2-44bc-b731-db5d42dfc791"));
        assert history != null;
        assert history.getNameAt(1000L).equals("GOden02");
        assert history.getNameAt(1457572526000L).equals("MidnightGoden");
    }
}
