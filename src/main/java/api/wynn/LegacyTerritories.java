package api.wynn;

import api.wynn.structs.TerritoryList;
import log.Logger;
import utils.HttpUtils;
import utils.rateLimit.RateLimiter;

import javax.annotation.Nullable;
import java.util.TimeZone;

/**
 * Legacy API
 * GET https://api.wynncraft.com/public_api.php?action=territoryList
 */
class LegacyTerritories {
    private static final String territoryListUrl = "https://api.wynncraft.com/public_api.php?action=territoryList";

    private final RateLimiter rateLimiter;
    private final Logger logger;
    private final TimeZone wynnTimeZone;

    LegacyTerritories(RateLimiter rateLimiter, Logger logger, TimeZone wynnTimeZone) {
        this.rateLimiter = rateLimiter;
        this.logger = logger;
        this.wynnTimeZone = wynnTimeZone;
    }

    @Nullable
    TerritoryList mustGetTerritoryList() {
        this.rateLimiter.stackUpRequest();

        try {
            long start = System.nanoTime();
            String body = HttpUtils.get(territoryListUrl);
            long end = System.nanoTime();
            this.logger.debug(String.format("Wynn API: Requested territory list, took %s ms.", (double) (end - start) / 1_000_000d));

            if (body == null) throw new Exception("returned body was null");
            return new TerritoryList(body, this.wynnTimeZone);
        } catch (Exception e) {
            this.logger.logException("an exception occurred while requesting / parsing territory list", e);
            return null;
        }
    }
}
