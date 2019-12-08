package api;

import api.structs.OnlinePlayers;
import api.structs.TerritoryList;
import log.Logger;
import utils.HttpUtils;

import javax.annotation.Nullable;
import java.util.TimeZone;

public class WynnApi {
    private final Logger logger;
    private final TimeZone wynnTimeZone;

    public WynnApi(Logger logger, TimeZone wynnTimeZone) {
        this.logger = logger;
        this.wynnTimeZone = wynnTimeZone;
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
            return new TerritoryList(body, this.wynnTimeZone);
        } catch (Exception e) {
            this.logger.logError("an error occurred while requesting territory list", e);
            return null;
        }
    }
}
