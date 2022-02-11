package api.mojang;

import api.mojang.structs.NameHistory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import log.Logger;
import org.jetbrains.annotations.Nullable;
import utils.HttpUtils;
import utils.UUID;
import utils.cache.DataCache;
import utils.cache.HashMapDataCache;
import utils.rateLimit.RateLimiter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

class UsernameToUUID {
    private static final ObjectMapper mapper = new ObjectMapper();

    private static final String USERNAME_TO_UUID_AT_TIME = "https://api.mojang.com/users/profiles/minecraft/%s?at=%s";
    private static final String UUID_TO_NAME_HISTORY = "https://api.mojang.com/user/profiles/%s/names";

    // username -> name history
    private static final DataCache<String, NameHistory> nameHistoryCache = new HashMapDataCache<>(
            1000, TimeUnit.HOURS.toMillis(3), TimeUnit.HOURS.toMillis(3)
    );
    // UUID (with hyphens) -> name history
    private static final DataCache<String, NameHistory> uuidToNameHistoryCache = new HashMapDataCache<>(
            100, TimeUnit.HOURS.toMillis(3), TimeUnit.HOURS.toMillis(3)
    );

    private final RateLimiter rateLimiter;
    private final Logger logger;

    UsernameToUUID(RateLimiter rateLimiter, Logger logger) {
        this.rateLimiter = rateLimiter;
        this.logger = logger;
    }

    @Nullable
    private UUID requestUUIDAtTime(String username, long unixMillis) {
        try {
            long start = System.nanoTime();
            String url = String.format(USERNAME_TO_UUID_AT_TIME, username, unixMillis / 1000);
            String data = HttpUtils.get(url);
            long end = System.nanoTime();

            this.logger.debug(String.format("Mojang API: Requested UUID of %s at %s, took %s ms",
                    username, unixMillis / 1000, (double) (end - start) / 1_000_000d));

            if (data == null) {
                return null;
            }

            JsonNode node = mapper.readTree(data);
            if (!node.has("id")) {
                return null;
            }

            return new UUID(node.get("id").asText());
        } catch (Exception e) {
            this.logger.logException("Something went wrong while requesting Mojang API", e);
            return null;
        }
    }

    @Nullable
    UUID mustGetUUIDAtTime(String username, long unixMillis) {
        NameHistory history = nameHistoryCache.get(username);
        if (history != null && history.getNameAt(unixMillis).equals(username)) {
            return history.uuid();
        }

        rateLimiter.stackUpRequest();
        return this.requestUUIDAtTime(username, unixMillis);
    }

    @Nullable
    private NameHistory requestNameHistory(UUID uuid) {
        try {
            long start = System.nanoTime();
            String url = String.format(UUID_TO_NAME_HISTORY, uuid.toString());
            String data = HttpUtils.get(url);
            long end = System.nanoTime();

            this.logger.debug(String.format("Mojang API: Requested name history of %s, took %s ms",
                    uuid.toStringWithHyphens(), (double) (end - start) / 1_000_000d));

            if (data == null) {
                return null;
            }

            // parse data
            JsonNode node = mapper.readTree(data);
            List<NameHistory.NameHistoryEntry> history = new ArrayList<>(node.size());
            NameHistory ret = new NameHistory(uuid, history);
            for (int i = 0; i < node.size(); i++) {
                JsonNode child = node.get(i);
                long changedToAt;
                if (child.has("changedToAt")) {
                    changedToAt = child.get("changedToAt").asLong();
                } else {
                    changedToAt = 0L;
                }
                String username = child.get("name").asText();
                history.add(new NameHistory.NameHistoryEntry(
                        username, changedToAt
                ));

                // add to cache
                nameHistoryCache.add(username, ret);
            }

            // add to cache
            uuidToNameHistoryCache.add(uuid.toStringWithHyphens(), ret);

            return ret;
        } catch (Exception e) {
            this.logger.logException("Something went wrong while requesting Mojang API", e);
            return null;
        }
    }

    @Nullable
    NameHistory mustGetNameHistory(UUID uuid) {
        NameHistory history = uuidToNameHistoryCache.get(uuid.toStringWithHyphens());
        if (history != null) {
            return history;
        }

        rateLimiter.stackUpRequest();
        return requestNameHistory(uuid);
    }
}
