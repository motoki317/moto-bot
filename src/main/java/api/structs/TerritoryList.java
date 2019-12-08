package api.structs;

import api.structs.common.Request;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TimeZone;

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

    public TerritoryList(String body, TimeZone wynnTimeZone) throws JsonProcessingException {
        JsonNode json = mapper.readTree(body);

        for (Iterator<Map.Entry<String, JsonNode>> i = json.fields(); i.hasNext(); ) {
            Map.Entry<String, JsonNode> e = i.next();

            if (e.getKey().equals("request")) {
                this.request = mapper.readValue(e.getValue().toString(), Request.class);
                continue;
            }

            if (e.getKey().equals("territories")) {
                this.territories = parseTerritories(e.getValue(), wynnTimeZone);
            }
        }
    }

    private static Map<String, Territory> parseTerritories(JsonNode json, TimeZone wynnTimeZone) throws JsonProcessingException {
        Map<String, Territory> territories = new HashMap<>();

        for (Iterator<Map.Entry<String, JsonNode>> i = json.fields(); i.hasNext(); ) {
            Map.Entry<String, JsonNode> e = i.next();

            String territoryName = e.getKey();
            Territory territory = Territory.parse(e.getValue().toString(), wynnTimeZone);

            territories.put(territoryName, territory);
        }
        return territories;
    }
}
