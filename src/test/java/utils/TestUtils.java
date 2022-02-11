package utils;

import log.ConsoleLogger;
import log.Logger;
import org.jetbrains.annotations.TestOnly;

public class TestUtils {
    @TestOnly
    public static Logger getLogger() {
        return new ConsoleLogger();
    }
}
