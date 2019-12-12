package utils;

import net.dv8tion.jda.api.entities.User;

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
     * @return Formatted string.
     */
    public static String getReadableDHMSFormat(long seconds, boolean formatSpaces) {
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
            if (e.getKey() != 0 || formatStarted) {
                formatStarted = true;
                if (formatSpaces) {
                    formattedStrings.add(String.format("%2s %s", e.getKey(), e.getValue()));
                } else {
                    formattedStrings.add(e.getKey() + " " + e.getValue());
                }
            }
        }

        return String.join(" ", formattedStrings);
    }
}
