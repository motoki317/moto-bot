package utils;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

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

    @Test
    void testParseReadableFormat() {
        assert FormatUtils.parseReadableTime("1 h 1 m 13 s") == 3673L;
        assert FormatUtils.parseReadableTime(" 1 h  1 m 13 s") == 3673L;
        assert FormatUtils.parseReadableTime("1 d 0 h 0 m 0 s") == 86400L;
        assert FormatUtils.parseReadableTime("0 s") == 0L;
    }

    @Test
    void testTruncateNumber() {
        assert "1.235M".equals(FormatUtils.truncateNumber(new BigDecimal(1_234_567)));
        assert "1.234M".equals(FormatUtils.truncateNumber(new BigDecimal(1_234_432)));
        assert "1.234K".equals(FormatUtils.truncateNumber(new BigDecimal(1_234)));
        assert "3.141K".equals(FormatUtils.truncateNumber(new BigDecimal(3_141)));
        assert "5.678B".equals(FormatUtils.truncateNumber(new BigDecimal(5_678_234_432L)));
    }
}
