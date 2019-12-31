package db.model.timezone;

import org.jetbrains.annotations.NotNull;

import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class CustomTimeZone implements CustomTimeZoneId {
    public static CustomTimeZone getDefault() {
        return new CustomTimeZone(0, "GMT+0");
    }

    private long discordId;
    @NotNull
    private String timezone;

    public CustomTimeZone(long discordId, @NotNull String timezone) {
        this.discordId = discordId;
        this.timezone = timezone;
    }

    @Override
    public long getDiscordId() {
        return discordId;
    }

    @NotNull
    public String getTimezone() {
        return timezone;
    }

    @NotNull
    public TimeZone getTimeZoneInstance() {
        return TimeZone.getTimeZone(this.timezone);
    }

    /**
     * Returns formatted time.
     * Examples: `+9`, `-0530`
     * @return Formatted time.
     */
    @NotNull
    public String getFormattedTime() {
        long offset = this.getTimeZoneInstance().getOffset(System.currentTimeMillis());
        if (offset % TimeUnit.HOURS.toMillis(1) == 0) {
            return String.format("%+d", offset / TimeUnit.HOURS.toMillis(1));
        }
        String sign = offset >= 0 ? "+" : "-";
        offset = Math.abs(offset);
        long hours = offset / TimeUnit.HOURS.toMillis(1);
        offset -= hours * TimeUnit.HOURS.toMillis(1);
        long minutes = offset / TimeUnit.MINUTES.toMillis(1);
        return String.format("%s%02d%02d", sign, hours, minutes);
    }
}
