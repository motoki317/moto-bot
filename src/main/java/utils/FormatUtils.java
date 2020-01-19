package utils;

import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class FormatUtils {
    /**
     * Returns user name with its discriminator.
     * @param user User object.
     * @return Example "username#1234"
     */
    public static String getUserFullName(User user) {
        return user.getName() + "#" + user.getDiscriminator();
    }

    /**
     * Formats given time in human-readable Days, Hours, Minutes, Seconds format. <br/>
     * <b>Example Input</b> <br/>
     * 3673 seconds <br/>
     * <b>Example Output</b> <br/>
     * " 1 h  1 m 13 s" (formatSpaces true) <br/>
     * "1 h 1 m 13 s" (formatSpaces false) <br/>
     * @param seconds Number of seconds.
     * @param formatSpaces Right-justify spaces.
     * @param smallestUnit Specifies smallest unit. For example if "m" was given, returns "1 h 1 m". Can be chosen from
     *                     "d", "h", "m", and "s".
     * @return Formatted string.
     */
    public static String formatReadableTime(long seconds, boolean formatSpaces, @NotNull String smallestUnit) {
        long days = seconds / TimeUnit.DAYS.toSeconds(1);
        long rem = seconds - days * TimeUnit.DAYS.toSeconds(1);

        long hours = rem / TimeUnit.HOURS.toSeconds(1);
        rem -= hours * TimeUnit.HOURS.toSeconds(1);

        long minutes = rem / TimeUnit.MINUTES.toSeconds(1);
        rem -= minutes * TimeUnit.MINUTES.toSeconds(1);

        List<Map.Entry<Long, String>> toFormat = new ArrayList<>();
        List<String> formattedStrings = new ArrayList<>();
        toFormat.add(new AbstractMap.SimpleEntry<>(days, "d"));
        toFormat.add(new AbstractMap.SimpleEntry<>(hours, "h"));
        toFormat.add(new AbstractMap.SimpleEntry<>(minutes, "m"));
        toFormat.add(new AbstractMap.SimpleEntry<>(rem, "s"));

        boolean formatStarted = false;
        for (Map.Entry<Long, String> e : toFormat) {
            if (e.getKey() != 0 || formatStarted || e.getValue().equals(smallestUnit)) {
                formatStarted = true;
                if (formatSpaces) {
                    formattedStrings.add(String.format("%2s %s", e.getKey(), e.getValue()));
                } else {
                    formattedStrings.add(e.getKey() + " " + e.getValue());
                }
                if (e.getValue().equals(smallestUnit)) {
                    break;
                }
            }
        }

        return String.join(" ", formattedStrings);
    }

    private static final BigDecimal THOUSAND = new BigDecimal("1000");
    private static final BigDecimal MILLION = THOUSAND.multiply(THOUSAND);
    private static final BigDecimal BILLION = MILLION.multiply(THOUSAND);
    private static final BigDecimal TRILLION = BILLION.multiply(THOUSAND);
    private static final BigDecimal QUADRILLION = TRILLION.multiply(THOUSAND);

    /**
     * Truncates a number to at most 6-length string, with possible unit at last.
     * <br>Example input: 1234567, Output: 1.235M
     * @param number Number to format.
     * @return Formatted string.
     */
    public static String truncateNumber(BigDecimal number) {
        if (number.compareTo(THOUSAND) >= 0 && number.compareTo(MILLION) < 0) {
            BigDecimal answer = number.divide(THOUSAND, 3, BigDecimal.ROUND_HALF_UP);
            int scale = (answer.precision() - answer.scale());
            return answer.setScale(4-scale, BigDecimal.ROUND_HALF_UP) + "K";
        } else if (number.compareTo(MILLION) >= 0 && number.compareTo(BILLION) < 0) {
            BigDecimal answer = number.divide(MILLION, 3, BigDecimal.ROUND_HALF_UP);
            int scale = (answer.precision() - answer.scale());
            return answer.setScale(4-scale, BigDecimal.ROUND_HALF_UP) + "M";
        } else if (number.compareTo(BILLION) >= 0 && number.compareTo(TRILLION) < 0) {
            BigDecimal answer = number.divide(BILLION, 3, BigDecimal.ROUND_HALF_UP);
            int scale = (answer.precision() - answer.scale());
            return answer.setScale(4-scale, BigDecimal.ROUND_HALF_UP) + "B";
        } else if (number.compareTo(TRILLION) >= 0 && number.compareTo(QUADRILLION) < 0) {
            BigDecimal answer = number.divide(TRILLION, 3, BigDecimal.ROUND_HALF_UP);
            int scale = (answer.precision() - answer.scale());
            return answer.setScale(4-scale, BigDecimal.ROUND_HALF_UP) + "T";
        } else if (number.compareTo(QUADRILLION) >= 0) {
            BigDecimal answer = number.divide(THOUSAND, 3, BigDecimal.ROUND_HALF_UP);
            int scale = (answer.precision() - answer.scale());
            return answer.setScale(Math.max(0,4-scale), BigDecimal.ROUND_HALF_UP) + "K";
        }
        return number.toString();
    }
}
