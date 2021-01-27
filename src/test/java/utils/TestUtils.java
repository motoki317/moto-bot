package utils;

import log.ConsoleLogger;
import log.Logger;
import org.jetbrains.annotations.TestOnly;

import java.util.TimeZone;

public class TestUtils {
    @TestOnly
    public static TimeZone getLogTimeZone() {
        return TimeZone.getTimeZone("Asia/Tokyo");
    }

    @TestOnly
    public static Logger getLogger() {
        return new ConsoleLogger(getLogTimeZone());
    }
}
