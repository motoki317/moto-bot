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
    public record Range(@NotNull Date start, @NotNull Date end) {
    }

    /**
     * Parses range from the given arguments.
     *
     * @param parsedArgs Parsed arguments in form of map. e.g. {"-since": "2020-01-01 12:00:00", "-until", "2020-01-05 12:00:00"} and so on.
     * @param timeZone   Time zone to parse with.
     * @param maxRange   Max-range in milliseconds. If the parsed range exceeds this range, an exception is thrown.
     * @return Time range.
     * @throws IllegalArgumentException On parse error, i.e. argument like "-since" was specified but unable to parse it.
     * @see ArgumentParser for parsedArgs argument.
     */
    @Nullable
    public static Range parseRange(Map<String, String> parsedArgs, TimeZone timeZone, @Nullable Long maxRange) throws IllegalArgumentException {
        // Specify range with "--days" argument
        Range range = parseRangeByDays(parsedArgs);

        // Specify range with "--since" and "--until" argument
        if (range == null) {
            range = parseRangeByTime(parsedArgs, timeZone);
        }

        // Validate the outcome
        if (range != null) {
            if (range.start.after(range.end)) {
                throw new IllegalArgumentException("Since date comes after the specified until date.");
            }
            if (maxRange != null && (range.end.getTime() - range.start.getTime()) > maxRange) {
                throw new IllegalArgumentException(String.format("Too large date difference (max: %s days).",
                        maxRange / TimeUnit.DAYS.toMillis(1)));
            }
        }

        return range;
    }

    @Nullable
    private static Range parseRangeByDays(Map<String, String> parsedArgs) throws IllegalArgumentException {
        if (parsedArgs.containsKey("d") || parsedArgs.containsKey("-days")) {
            int days = InputChecker.getPositiveInteger(parsedArgs.get("d") != null
                    ? parsedArgs.get("d") : parsedArgs.get("-days"));

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
            return new Range(since, until);
        }
        return null;
    }

    private record TimePattern(Pattern pattern, TimeUnit unit) {
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
