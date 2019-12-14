package utils;

import org.junit.jupiter.api.Test;

class TestFormatUtils {
    @Test
    void testGetReadableDHMSFormat() {
        long seconds = 3673L;
        assert "1 h 1 m 13 s".equals(FormatUtils.formatReadableTime(seconds, false, "s"));
        assert " 1 h  1 m 13 s".equals(FormatUtils.formatReadableTime(seconds, true, "s"));
        assert "1 d 0 h 0 m 0 s".equals(FormatUtils.formatReadableTime(86400L, false, "s"));
        assert "1 d".equals(FormatUtils.formatReadableTime(86400L, false, "d"));
        assert "10 m".equals(FormatUtils.formatReadableTime(630L, false, "m"));
        assert " 1 h 10 m".equals(FormatUtils.formatReadableTime(4230L, true, "m"));
        assert "0 s".equals(FormatUtils.formatReadableTime(0L, false, "s"));
    }
}
