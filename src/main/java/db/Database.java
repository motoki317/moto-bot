package db;

import db.repository.base.*;
import org.jetbrains.annotations.NotNull;

public interface Database {
    @NotNull
    TrackChannelRepository getTrackingChannelRepository();
    @NotNull
    WorldRepository getWorldRepository();
    @NotNull
    CommandLogRepository getCommandLogRepository();
    @NotNull
    TerritoryRepository getTerritoryRepository();
    @NotNull
    TerritoryLogRepository getTerritoryLogRepository();
    @NotNull
    WarTrackRepository getWarTrackRepository();
    @NotNull
    WarLogRepository getWarLogRepository();
    @NotNull
    WarPlayerRepository getWarPlayerRepository();
    @NotNull
    TimeZoneRepository getTimeZoneRepository();
    @NotNull
    PrefixRepository getPrefixRepository();
    @NotNull
    GuildWarLogRepository getGuildWarLogRepository();
    @NotNull
    DateFormatRepository getDateFormatRepository();
    @NotNull
    GuildRepository getGuildRepository();
    @NotNull
    IgnoreChannelRepository getIgnoreChannelRepository();
    @NotNull
    GuildLeaderboardRepository getGuildLeaderboardRepository();
    @NotNull
    GuildXpLeaderboardRepository getGuildXpLeaderboardRepository();
    @NotNull
    GuildWarLeaderboardRepository getGuildWarLeaderboardRepository();
    @NotNull
    PlayerWarLeaderboardRepository getPlayerWarLeaderboardRepository();
    @NotNull
    TerritoryListRepository getTerritoryListRepository();
    @NotNull
    GuildListRepository getGuildListRepository();
    @NotNull
    ServerLogRepository getServerLogRepository();
    @NotNull
    MusicSettingRepository getMusicSettingRepository();
    @NotNull
    MusicQueueRepository getMusicQueueRepository();
    @NotNull
    MusicInterruptedGuildRepository getMusicInterruptedGuildRepository();
    @NotNull
    PlayerNumberRepository getPlayerNumberRepository();
}
