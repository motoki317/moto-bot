package db.model.guild;

import org.jetbrains.annotations.NotNull;

import java.util.Date;

public class Guild implements GuildId {
    @NotNull
    private final String name;
    private final String prefix;
    private final Date createdAt;

    public Guild(@NotNull String name, String prefix, Date createdAt) {
        this.name = name;
        this.prefix = prefix;
        this.createdAt = createdAt;
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }

    public String getPrefix() {
        return prefix;
    }

    public Date getCreatedAt() {
        return createdAt;
    }
}
