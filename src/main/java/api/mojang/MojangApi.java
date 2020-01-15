package api.mojang;

import api.mojang.structs.NameToUUID;
import api.mojang.structs.NullableUUID;
import com.fasterxml.jackson.databind.ObjectMapper;
import log.Logger;
import utils.HttpUtils;
import utils.UUID;
import utils.cache.DataCache;
import utils.cache.HashMapDataCache;
import utils.rateLimit.RateLimiter;
import utils.rateLimit.WaitableRateLimiter;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MojangApi {
    private static final ObjectMapper mapper = new ObjectMapper();

    // ----- Rate limiter -----

    // As of Dec 29th, 2019, the rate limit is 600 reqs / 10 minutes
    // So 1 req / s at max
    private static final int rateLimitPerTenMinutes = 600;
    private static final int maxRequestStacks = 5;
    private static final RateLimiter rateLimiter;

    static {
        long waitBetweenRequests = TimeUnit.MINUTES.toMillis(10) / rateLimitPerTenMinutes;
        System.out.println("Setting Mojang API minimum request wait time to " + waitBetweenRequests + " ms. " +
                "(i.e. " + rateLimitPerTenMinutes + " requests per 10 minutes)");

        rateLimiter = new WaitableRateLimiter("Mojang", waitBetweenRequests, maxRequestStacks);
    }

    // ----- Cache system -----

    private static final DataCache<String, NullableUUID> nameToUUIDCache = new HashMapDataCache<>(
            100, TimeUnit.MINUTES.toMillis(10), TimeUnit.MINUTES.toMillis(10)
    );

    // ----- API instance -----

    private final Logger logger;

    public MojangApi(Logger logger) {
        this.logger = logger;
    }

    private static final int NAME_TO_UUID_PLAYERS_PER_REQUEST = 10;
    private static final String NAME_TO_UUID_URL = "https://api.mojang.com/profiles/minecraft";

    @Nullable
    private static NullableUUID getUUIDUsingCache(String name) {
        NullableUUID uuid;
        if ((uuid = nameToUUIDCache.get(name)) != null) {
            return uuid;
        }
        return null;
    }

    /**
     * POST https://api.mojang.com/profiles/minecraft, given names are assumed to not be in cache.
     * @param names Player names (player readable form). Size has to be less than or equal to 10.
     * @return Map of player names to UUIDs.
     */
    @Nullable
    private Map<String, NullableUUID> getUUIDs(List<String> names) {
        if (names.isEmpty() || names.size() > NAME_TO_UUID_PLAYERS_PER_REQUEST) {
            throw new IllegalArgumentException("Length of list of names has to be less than or equal to " + NAME_TO_UUID_PLAYERS_PER_REQUEST);
        }

        try {
            rateLimiter.checkRequest();

            long start = System.nanoTime();
            String postBody = String.format(
                    "[%s]",
                    names.stream().map(n -> "\"" + n + "\"").collect(Collectors.joining(","))
            );
            String data = HttpUtils.postJson(NAME_TO_UUID_URL, postBody);
            long end = System.nanoTime();

            this.logger.debug(String.format("Mojang API: Requested names -> UUID for %s players, took %s ms",
                    names.size(), (double) (end - start) / 1_000_000d));

            if (data == null) return null;

            // parse data
            Map<String, NullableUUID> ret = new HashMap<>();
            NameToUUID[] parsed = mapper.readValue(data, NameToUUID[].class);
            Map<String, String> returnedUUIDs = Arrays.stream(parsed).collect(Collectors.toMap(NameToUUID::getName, NameToUUID::getId));

            for (String name : names) {
                if (returnedUUIDs.containsKey(name)) {
                    ret.put(name, new NullableUUID(new UUID(returnedUUIDs.get(name))));
                } else {
                    ret.put(name, new NullableUUID(null));
                }
            }

            // store cache
            ret.forEach(nameToUUIDCache::add);

            return ret;
        } catch (Exception e) {
            this.logger.logException("Something went wrong while requesting Mojang API", e);
            return null;
        }
    }

    /**
     * Iteratively calls getUUIDs method if size of given names is larger than 10,
     * but preferably not too large (not larger than 100).
     * @param names List of names.
     * @return Map of player names to UUIDs. null if something went wrong.
     */
    @Nullable
    public Map<String, NullableUUID> getUUIDsIterative(List<String> names) {
        Map<String, NullableUUID> ret = new HashMap<>();
        List<String> namesToRequest = new ArrayList<>();
        for (String name : names) {
            NullableUUID uuid;
            if ((uuid = getUUIDUsingCache(name)) != null) {
                ret.put(name, uuid);
            } else {
                namesToRequest.add(name);
            }
        }

        if (namesToRequest.isEmpty()) {
            return ret;
        }

        int count = ((namesToRequest.size() - 1) / 10) + 1;
        for (int i = 0; i < count; i++) {
            int start = i * NAME_TO_UUID_PLAYERS_PER_REQUEST;
            int end = Math.min((i + 1) * NAME_TO_UUID_PLAYERS_PER_REQUEST, namesToRequest.size());

            Map<String, NullableUUID> res = getUUIDs(namesToRequest.subList(start, end));
            if (res == null) {
                return null;
            }

            ret.putAll(res);
        }
        return ret;
    }
}
