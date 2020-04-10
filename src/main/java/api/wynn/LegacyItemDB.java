package api.wynn;

import api.wynn.structs.ItemDB;
import com.fasterxml.jackson.databind.ObjectMapper;
import log.Logger;
import org.jetbrains.annotations.Nullable;
import utils.HttpUtils;
import utils.rateLimit.RateLimiter;

class LegacyItemDB {
    private static final String ITEM_DB_URL = "https://api.wynncraft.com/public_api.php?action=itemDB&category=all";
    private static final ObjectMapper mapper = new ObjectMapper();

    @Nullable
    private static ItemDB cache;

    private final RateLimiter rateLimiter;
    private final Logger logger;

    LegacyItemDB(RateLimiter rateLimiter, Logger logger) {
        this.rateLimiter = rateLimiter;
        this.logger = logger;
    }

    @Nullable
    ItemDB mustGetItemDB(boolean forceReload) {
        if (!forceReload && cache != null) {
            return cache;
        }

        return requestItemDB();
    }

    @Nullable
    private ItemDB requestItemDB() {
        this.rateLimiter.stackUpRequest();

        try {
            long start = System.nanoTime();
            String body = HttpUtils.get(ITEM_DB_URL);
            long end = System.nanoTime();
            this.logger.debug(String.format("Wynn API: Requested item list, took %s ms.", (double) (end - start) / 1_000_000d));

            if (body == null) throw new Exception("returned body was null");

            ItemDB db = mapper.readValue(body, ItemDB.class);
            if (db == null) throw new Exception("an exception occurred while parsing item list");

            cache = db;
            return db;
        } catch (Exception e) {
            this.logger.logException("an exception occurred while requesting / parsing item list", e);
            return null;
        }
    }
}
