package api.thecatapi;

import api.thecatapi.structs.CatResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import log.Logger;
import org.jetbrains.annotations.Nullable;
import utils.HttpUtils;
import utils.rateLimit.RateLimiter;
import utils.rateLimit.WaitableRateLimiter;

import java.io.IOException;

public class TheCatApi {
    private final static ObjectMapper mapper = new ObjectMapper();

    private final Logger logger;
    private final RateLimiter rateLimiter;

    public TheCatApi(Logger logger) {
        this.logger = logger;
        this.rateLimiter = new WaitableRateLimiter("The Cat", 1000L, 5);
    }

    private static final String THE_CAT_API_URL = "https://api.thecatapi.com/v1/images/search";

    @Nullable
    public CatResponse mustGetCat() {
        this.rateLimiter.stackUpRequest();
        return requestCat();
    }

    @Nullable
    private CatResponse requestCat() {
        try {
            long start = System.nanoTime();
            String body = HttpUtils.get(THE_CAT_API_URL);
            long end = System.nanoTime();
            this.logger.debug(String.format("The Cat API: Requested a cat image, took %s ms",
                    (double) (end - start) / 1_000_000d));

            if (body == null) {
                return null;
            }

            CatResponse[] res = mapper.readValue(body, CatResponse[].class);
            if (res == null || res.length == 0) {
                return null;
            }
            return res[0];
        } catch (IOException e) {
            this.logger.logException("Something went wrong while requesting The Cat API.", e);
            return null;
        }
    }
}
