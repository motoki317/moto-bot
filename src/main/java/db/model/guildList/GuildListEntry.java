package db.model.guildList;

import org.jetbrains.annotations.NotNull;

public class GuildListEntry implements GuildListEntryId {
    private long userId;
    @NotNull
    private String listName;
    @NotNull
    private String guildName;

    public GuildListEntry(long userId, @NotNull String listName, @NotNull String guildName) {
        this.userId = userId;
        this.listName = listName;
        this.guildName = guildName;
    }

    public long getUserId() {
        return userId;
    }

    @NotNull
    public String getListName() {
        return listName;
    }

    @NotNull
    public String getGuildName() {
        return guildName;
    }
}
