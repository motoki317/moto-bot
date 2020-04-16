package utils;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.junit.jupiter.api.Test;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static utils.RangeParser.Range;
import static utils.RangeParser.parseRange;

class RangeParserTest {
    private static final TimeZone utc = TimeZone.getTimeZone("UTC");

    @TestOnly
    private Map<String, String> createMap(String... keysAndValues) {
        assertEquals(0, keysAndValues.length % 2);

        Map<String, String> ret = new HashMap<>();
        for (int i = 0; i < keysAndValues.length / 2; i++) {
            ret.put(keysAndValues[i * 2], keysAndValues[i * 2 + 1]);
        }
        return ret;
    }

    @Test
    void testRanged() throws ParseException {
        class TestCase {
            private Map<String, String> args;
            private long expected;
            @Nullable
            private Date expectedSince;
            @Nullable
            private Date expectedUntil;

            private TestCase(Map<String, String> args, long expected,
                             @Nullable Date expectedSince, @Nullable Date expectedUntil) {
                this.args = args;
                this.expected = expected;
                this.expectedSince = expectedSince;
                this.expectedUntil = expectedUntil;
            }
        }

        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        dateFormat.setTimeZone(utc);

        TestCase[] cases = new TestCase[]{
                // days argument
                new TestCase(
                        createMap("d", "3"),
                        TimeUnit.DAYS.toMillis(3),
                        null, null
                ),
                new TestCase(
                        createMap("-days", "3"),
                        TimeUnit.DAYS.toMillis(3),
                        null, null
                ),
                new TestCase(
                        createMap("d", "15"),
                        TimeUnit.DAYS.toMillis(15),
                        null, null
                ),
                new TestCase(
                        createMap("-days", "15"),
                        TimeUnit.DAYS.toMillis(15),
                        null, null
                ),
                // specify via intuitive arguments
                new TestCase(
                        createMap("S", "1 day ago"),
                        TimeUnit.DAYS.toMillis(1),
                        null, null
                ),
                new TestCase(
                        createMap("-since", "15 hours ago"),
                        TimeUnit.HOURS.toMillis(15),
                        null, null
                ),
                new TestCase(
                        createMap("-since", "17 hours ago", "-until", "3 hours ago"),
                        TimeUnit.HOURS.toMillis(14),
                        null, null
                ),
                // specify exact date and time
                new TestCase(
                        createMap("S", "2020-01-01 00:00:00", "U", "2020-01-15 00:00:00"),
                        TimeUnit.DAYS.toMillis(14),
                        dateFormat.parse("2020/01/01 00:00:00"), dateFormat.parse("2020/01/15 00:00:00")
                ),
                new TestCase(
                        createMap("S", "2020/02/01 00:00:00", "U", "2020/02/15 00:00:00"),
                        TimeUnit.DAYS.toMillis(14),
                        dateFormat.parse("2020/02/01 00:00:00"), dateFormat.parse("2020/02/15 00:00:00")
                ),
                new TestCase(
                        createMap("S", "2020/01/01 00:00:00", "U", "2020/01/15 00:00:00"),
                        TimeUnit.DAYS.toMillis(14),
                        dateFormat.parse("2020/01/01 00:00:00"), dateFormat.parse("2020/01/15 00:00:00")
                ),
                new TestCase(
                        createMap("S", "2019-03-01", "U", "2019-03-15"),
                        TimeUnit.DAYS.toMillis(14),
                        dateFormat.parse("2019/03/01 00:00:00"), dateFormat.parse("2019/03/15 00:00:00")
                ),
                new TestCase(
                        createMap("S", "2020/03/01", "U", "2020/03/15"),
                        TimeUnit.DAYS.toMillis(14),
                        dateFormat.parse("2020/03/01 00:00:00"), dateFormat.parse("2020/03/15 00:00:00")
                ),
                new TestCase(
                        createMap("S", "2020/04/01", "U", "2020/04/06 12:00:00"),
                        TimeUnit.DAYS.toMillis(5) + TimeUnit.HOURS.toMillis(12),
                        dateFormat.parse("2020/04/01 00:00:00"), dateFormat.parse("2020/04/06 12:00:00")
                )
        };

        for (TestCase t : cases) {
            Range range = parseRange(t.args, utc, null);
            String caseStr = String.format("Test case %s", t.args);
            assertNotNull(range, caseStr);
            assertEquals(t.expected, range.end.getTime() - range.start.getTime(), caseStr);
            if (t.expectedSince != null) {
                assertEquals(t.expectedSince, range.start, caseStr);
            }
            if (t.expectedUntil != null) {
                assertEquals(t.expectedUntil, range.end, caseStr);
            }
        }
    }

    @Test
    void testMaxRange() {
        long MAX_RANGE = TimeUnit.DAYS.toMillis(31);

        assertDoesNotThrow(() -> parseRange(
                createMap("S", "2020-04-01", "U", "2020-04-05"),
                utc,
                MAX_RANGE
        ));
        assertThrows(IllegalArgumentException.class, () -> parseRange(
                createMap("S", "2019-04-01", "U", "2020-04-05"),
                utc,
                MAX_RANGE
        ));
    }
}
