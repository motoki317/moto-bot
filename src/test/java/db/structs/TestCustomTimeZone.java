package db.structs;

import db.model.timezone.CustomTimeZone;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

class TestCustomTimeZone {
    private static class TestCase {
        private String timezone;
        private String expectFormattedTime;
        private long expectOffset;

        private TestCase(String timezone, String expectFormattedTime, long expectOffset) {
            this.timezone = timezone;
            this.expectFormattedTime = expectFormattedTime;
            this.expectOffset = expectOffset;
        }
    }

    private static final long HOUR = TimeUnit.HOURS.toMillis(1);
    private static final long MINUTE = TimeUnit.MINUTES.toMillis(1);

    @Test
    void testFormattedTime() {
        TestCase[] cases = {
                new TestCase("GMT+9", "+9", HOUR * 9),
                new TestCase("GMT+09", "+9", HOUR * 9),
                new TestCase("GMT+0", "+0", 0),
                new TestCase("GMT+00", "+0", 0),
                new TestCase("GMT", "+0", 0),
                new TestCase("GMT-5", "-5", - HOUR * 5),
                new TestCase("GMT-05", "-5", - HOUR * 5),
                new TestCase("JST", "+9", HOUR * 9),
                new TestCase("GMT-0530", "-0530", - (HOUR * 5 + MINUTE * 30)),
                new TestCase("GMT-0905", "-0905", - (HOUR * 9 + MINUTE * 5)),
                new TestCase("Asia/Tokyo", "+9", HOUR * 9)
        };

        long now = System.currentTimeMillis();
        for (TestCase c : cases) {
            CustomTimeZone instance = new CustomTimeZone(0, c.timezone);
            assert instance.getFormattedTime().equals(c.expectFormattedTime);
            assert instance.getTimeZoneInstance().getOffset(now) == c.expectOffset;
        }
    }
}
