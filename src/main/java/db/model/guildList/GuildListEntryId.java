package db.model.guildList;

import org.jetbrains.annotations.NotNull;

public interface GuildListEntryId {
    long getUserId();
    @NotNull
    String getListName();
    @NotNull
    String getGuildName();
}
