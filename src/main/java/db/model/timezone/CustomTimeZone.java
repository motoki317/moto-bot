package db.model.timezone;

import org.jetbrains.annotations.NotNull;

public class CustomTimeZone implements CustomTimeZoneId {
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
}
