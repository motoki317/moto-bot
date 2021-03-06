package api.wynn.structs;

import api.wynn.structs.common.Request;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class TerritoryList {
    private static final ObjectMapper mapper = new ObjectMapper();

    private Request request;
    private Map<String, Territory> territories;

    public Request getRequest() {
        return request;
    }

    public Map<String, Territory> getTerritories() {
        return territories;
    }

    public TerritoryList(String body) throws JsonProcessingException {
        JsonNode json = mapper.readTree(body);

        for (Iterator<Map.Entry<String, JsonNode>> i = json.fields(); i.hasNext(); ) {
            Map.Entry<String, JsonNode> e = i.next();

            if (e.getKey().equals("request")) {
                this.request = mapper.readValue(e.getValue().toString(), Request.class);
                continue;
            }

            if (e.getKey().equals("territories")) {
                this.territories = parseTerritories(e.getValue());
            }
        }

        if (this.request == null) {
            throw new RuntimeException("Request field is null");
        }
        if (this.territories == null) {
            throw new RuntimeException("Territories field is null");
        }
    }

    private static Map<String, Territory> parseTerritories(JsonNode json) throws JsonProcessingException {
        Map<String, Territory> territories = new HashMap<>();

        for (Iterator<Map.Entry<String, JsonNode>> i = json.fields(); i.hasNext(); ) {
            Map.Entry<String, JsonNode> e = i.next();

            String territoryName = e.getKey();
            Territory territory = Territory.parse(e.getValue().toString());

            territories.put(territoryName, territory);
        }
        return territories;
    }
}
