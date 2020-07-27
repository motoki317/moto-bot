package db.model.dateFormat;

import org.jetbrains.annotations.NotNull;

public class CustomDateFormat implements CustomDateFormatId {
    public static CustomDateFormat getDefault() {
        return new CustomDateFormat(0, CustomFormat.TWENTY_FOUR_HOUR);
    }

    private final long discordId;
    @NotNull
    private final CustomFormat dateFormat;

    public CustomDateFormat(long discordId, @NotNull CustomFormat dateFormat) {
        this.discordId = discordId;
        this.dateFormat = dateFormat;
    }

    @Override
    public long getDiscordId() {
        return discordId;
    }

    @NotNull
    public CustomFormat getDateFormat() {
        return dateFormat;
    }
}
