package api.structs;

import api.structs.common.Request;

import java.util.Map;

public class TerritoryList {
    private Map<String, Territory> territories;
    private Request request;

    public Map<String, Territory> getTerritories() {
        return territories;
    }
}
