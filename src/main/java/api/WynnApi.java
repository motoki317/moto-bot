package api;

import api.structs.OnlinePlayers;
import api.structs.TerritoryList;
import com.fasterxml.jackson.databind.ObjectMapper;
import log.Logger;
import utils.HttpUtils;

import javax.annotation.Nullable;

public class WynnApi {
    private static final ObjectMapper mapper = new ObjectMapper();

    private final Logger logger;

    public WynnApi(Logger logger) {
        this.logger = logger;
    }

    private static final String onlinePlayersUrl = "https://api.wynncraft.com/public_api.php?action=onlinePlayers";

    /**
     * GET https://api.wynncraft.com/public_api.php?action=onlinePlayers
     * @return Online players struct.
     */
    @Nullable
    public OnlinePlayers getOnlinePlayers() {
        try {
            String body = HttpUtils.get(onlinePlayersUrl);
            return new OnlinePlayers(body);
        } catch (Exception e) {
            this.logger.logError("an error occurred while requesting online players", e);
            return null;
        }
    }

    private static final String territoryListUrl = "https://api.wynncraft.com/public_api.php?action=territoryList";

    /**
     * GET https://api.wynncraft.com/public_api.php?action=territoryList
     * @return Territory list struct.
     */
    @Nullable
    public TerritoryList getTerritoryList() {
        try {
            String body = HttpUtils.get(territoryListUrl);
            if (body == null) throw new Exception("returned body was null");
            return mapper.readValue(body, TerritoryList.class);
        } catch (Exception e) {
            this.logger.logError("an error occurred while requesting territory list", e);
            return null;
        }
    }
}
