package api.wynn.structs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

public class TerritoryList {
    private static final ObjectMapper mapper = new ObjectMapper();

    private final Map<String, Territory> territories;

    public Map<String, Territory> getTerritories() {
        return territories;
    }

    public TerritoryList(String body) throws JsonProcessingException {
        JsonNode json = mapper.readTree(body);

        this.territories = new HashMap<>();
        for (var fields = json.fields(); fields.hasNext(); ) {
            var field = fields.next();

            String territoryName = field.getKey();
            Territory territory = Territory.parse(territoryName, field.getValue().toString());

            territories.put(territoryName, territory);
        }
    }
}
