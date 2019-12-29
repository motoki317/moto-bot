package db.model.prefix;

import org.jetbrains.annotations.NotNull;

public class Prefix implements PrefixId {
    private long discordId;
    @NotNull
    private String prefix;

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
