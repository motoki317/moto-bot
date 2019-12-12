package utils;

import org.junit.jupiter.api.Test;

class TestFormatUtils {
    @Test
    void testGetReadableDHMSFormat() {
        long seconds = 3673L;
        assert "1 h 1 m 13 s".equals(FormatUtils.getReadableDHMSFormat(seconds, false));
        assert " 1 h  1 m 13 s".equals(FormatUtils.getReadableDHMSFormat(seconds, true));
        assert "1 d 0 h 0 m 0 s".equals(FormatUtils.getReadableDHMSFormat(86400L, false));
    }
}
