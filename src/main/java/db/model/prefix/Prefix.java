package db.model.prefix;

import org.jetbrains.annotations.NotNull;

public class Prefix implements PrefixId {
    private final long discordId;
    @NotNull
    private final String prefix;

    public Prefix(long discordId, @NotNull String prefix) {
        this.discordId = discordId;
        this.prefix = prefix;
    }

    @Override
    public long getDiscordId() {
        return discordId;
    }

    @NotNull
    public String getPrefix() {
        return prefix;
    }
}
