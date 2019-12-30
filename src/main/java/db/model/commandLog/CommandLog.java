package db.model.commandLog;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;

public class CommandLog implements CommandLogId {
    private int id;
    @NotNull
    private String kind;
    @NotNull
    private String full;
    @Nullable
    private Long guildId;
    private long channelId;
    private long userId;
    @NotNull
    private Date createdAt;

    // For select
    public CommandLog(int id, @NotNull String kind, @NotNull String full, @Nullable Long guildId, long channelId, long userId, @NotNull Date createdAt) {
        this.id = id;
        this.kind = kind;
        this.full = full;
        this.guildId = guildId;
        this.channelId = channelId;
        this.userId = userId;
        this.createdAt = createdAt;
    }

    // For insert
    public CommandLog(@NotNull String kind, @NotNull String full, @Nullable Long guildId, long channelId, long userId, @NotNull Date createdAt) {
        this.kind = kind;
        this.full = full;
        this.guildId = guildId;
        this.channelId = channelId;
        this.userId = userId;
        this.createdAt = createdAt;
    }

    @Override
    public int getId() {
        return id;
    }

    @NotNull
    public String getKind() {
        return kind;
    }

    @NotNull
    public String getFull() {
        return full;
    }

    @Nullable
    public Long getGuildId() {
        return guildId;
    }

    public long getChannelId() {
        return channelId;
    }

    public long getUserId() {
        return userId;
    }

    @NotNull
    public Date getCreatedAt() {
        return createdAt;
    }
}
