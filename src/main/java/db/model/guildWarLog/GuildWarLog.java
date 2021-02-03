package db.model.guildWarLog;

import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class GuildWarLog implements GuildWarLogId {
    private final int id;
    @NotNull
    private final String guildName;
    @Nullable
    private final Integer warLogId;
    @Nullable
    private final Integer territoryLogId;

    public GuildWarLog(int id, @NotNull String guildName, @Nullable Integer warLogId, @Nullable Integer territoryLogId) {
        this.id = id;
        this.guildName = guildName;
        this.warLogId = warLogId;
        this.territoryLogId = territoryLogId;
    }

    @Override
    public int getId() {
        return id;
    }

    @NotNull
    public String getGuildName() {
        return guildName;
    }

    @Nullable
    public Integer getWarLogId() {
        return warLogId;
    }

    @Nullable
    public Integer getTerritoryLogId() {
        return territoryLogId;
    }
}
