package db.model.dateFormat;

import org.jetbrains.annotations.NotNull;

public class CustomDateFormat implements CustomDateFormatId {
    private long discordId;
    @NotNull
    private CustomFormat dateFormat;

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
