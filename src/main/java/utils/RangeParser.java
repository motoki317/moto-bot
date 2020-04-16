package utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RangeParser {
    public static class Range {
        @NotNull
        public final Date start;
        @NotNull
        public final Date end;

        private Range(@NotNull Date start, @NotNull Date end) {
            this.start = start;
            this.end = end;
        }
    }

    @Nullable
    public static Range parseRange(Map<String, String> parsedArgs, TimeZone timeZone) throws IllegalArgumentException {
        // Specify range with "--days" argument
        Range range = parseRangeByDays(parsedArgs);
        if (range != null) {
            return range;
        }

        // Specify range with "--since" and "--until" argument
        range = parseRangeByTime(parsedArgs, timeZone);

        return range;
    }

    @Nullable
    private static Range parseRangeByDays(Map<String, String> parsedArgs) throws IllegalArgumentException {
        if (parsedArgs.containsKey("d") || parsedArgs.containsKey("-days")) {
            int days = InputChecker.getPositiveInteger(parsedArgs.get("d") != null
                    ? parsedArgs.get("d") : parsedArgs.get("-days"));
            if (days < 0 || days > 30) {
                throw new NumberFormatException("Please input an integer between 1 and 30 for days argument!");
            }

            Date now = new Date();
            Date old = new Date(now.getTime() - TimeUnit.DAYS.toMillis(days));
            return new Range(old, now);
        }
        return null;
    }

    @Nullable
    private static Range parseRangeByTime(Map<String, String> parsedArgs, TimeZone timeZone) throws IllegalArgumentException {
        if (parsedArgs.containsKey("-since") || parsedArgs.containsKey("S")) {
            long now = System.currentTimeMillis();

            String sinceStr = parsedArgs.get("-since") != null
                    ? parsedArgs.get("-since")
                    : parsedArgs.get("S");
            String untilStr = parsedArgs.get("-until") != null
                    ? parsedArgs.get("-until")
                    : parsedArgs.get("U");

            Date since = parseDate(sinceStr, timeZone, now);
            // If "until" argument is not specified, take current time
            Date until = untilStr == null
                    ? new Date(now)
                    : parseDate(untilStr, timeZone, now);
            if (since == null || until == null) {
                throw new IllegalArgumentException("Failed to parse since or until arguments. Please input a valid date.");
            }

            if (since.after(until)) {
                throw new IllegalArgumentException("Since date comes after the specified until date.");
            }

            if ((until.getTime() - since.getTime()) > MAX_RANGE) {
                throw new IllegalArgumentException(String.format("Too large date difference (max: %s days).", MAX_DAYS));
            }
            return new Range(since, until);
        }
        return null;
    }

    private final static int MAX_DAYS = 32;
    private final static long MAX_RANGE = TimeUnit.DAYS.toMillis(MAX_DAYS);

    private static class TimePattern {
        private final Pattern pattern;
        private final TimeUnit unit;

        private TimePattern(Pattern pattern, TimeUnit unit) {
            this.pattern = pattern;
            this.unit = unit;
        }
    }

    private final static List<DateFormat> acceptableFormats;
    private final static List<TimePattern> timePatterns;

    static {
        acceptableFormats = new ArrayList<>();
        // Try more precise formats first
        acceptableFormats.add(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        acceptableFormats.add(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss"));
        acceptableFormats.add(new SimpleDateFormat("yyyy-MM-dd"));
        acceptableFormats.add(new SimpleDateFormat("yyyy/MM/dd"));

        timePatterns = new ArrayList<>();
        timePatterns.add(new TimePattern(
                Pattern.compile("(\\d+) days? ago"),
                TimeUnit.DAYS
        ));
        timePatterns.add(new TimePattern(
                Pattern.compile("(\\d+) hours? ago"),
                TimeUnit.HOURS
        ));
        timePatterns.add(new TimePattern(
                Pattern.compile("(\\d+) minutes? ago"),
                TimeUnit.MINUTES
        ));
    }

    @Nullable
    private synchronized static Date parseDate(String str, TimeZone timeZone, long now) {
        for (DateFormat format : acceptableFormats) {
            format.setTimeZone(timeZone);
            try {
                Date d = format.parse(str);
                if (d != null) {
                    return d;
                }
            } catch (ParseException ignored) {
            }
        }

        for (TimePattern p : timePatterns) {
            Matcher m = p.pattern.matcher(str);
            if (m.matches()) {
                int num = Integer.parseInt(m.group(1));
                return new Date(
                        now - p.unit.toMillis(num)
                );
            }
        }
        return null;
    }
}
