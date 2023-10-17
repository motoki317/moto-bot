package api.wynn.structs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

public class OnlinePlayers {
    private static final ObjectMapper mapper = new ObjectMapper();

    private final Date retrievedAt;
    private final int total;
    // world name -> player list
    private final Map<String, List<String>> worlds;
    // player name -> world name
    private final Map<String, String> players;

    public OnlinePlayers(String body) throws JsonProcessingException {
        JsonNode json = mapper.readTree(body);

        this.retrievedAt = new Date();
        this.total = json.get("total").asInt();
        this.worlds = new HashMap<>();
        this.players = new HashMap<>();

        for (var fields = json.get("players").fields(); fields.hasNext(); ) {
            var field = fields.next();
            var player = field.getKey();
            var world = field.getValue().asText();

            if (!this.worlds.containsKey(world)) {
                this.worlds.put(world, new ArrayList<>());
            }
            this.worlds.get(world).add(player);
            this.players.put(player, world);
        }
    }

    public Date getRetrievedAt() {
        return retrievedAt;
    }

    public int getTotal() {
        return total;
    }

    public Map<String, List<String>> getWorlds() {
        return worlds;
    }

    public Map<String, String> getPlayers() {
        return players;
    }
}
