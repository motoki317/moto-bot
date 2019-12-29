package api.wynn.structs;

import api.wynn.structs.common.Request;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class OnlinePlayers {
    private static final ObjectMapper mapper = new ObjectMapper();

    private Request request;
    // player list has to be bound by hand
    private Map<String, List<String>> worlds;

    public Request getRequest() {
        return request;
    }

    public Map<String, List<String>> getWorlds() {
        return worlds;
    }

    public OnlinePlayers(String body) throws JsonProcessingException {
        JsonNode json = mapper.readTree(body);

        this.worlds = new HashMap<>();

        for (Iterator<Map.Entry<String, JsonNode>> i = json.fields(); i.hasNext(); ) {
            Map.Entry<String, JsonNode> e = i.next();

            if (e.getKey().equals("request")) {
                this.request = mapper.readValue(e.getValue().toString(), Request.class);
                continue;
            }

            List<String> players = mapper.readValue(e.getValue().toString(), new TypeReference<List<String>>(){});
            this.worlds.put(e.getKey(), players);
        }
    }
}
