package db;

import db.repository.*;
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
    CustomTimeZoneRepository getCustomTimeZoneRepository();
    @NotNull
    PrefixRepository getPrefixRepository();
    @NotNull
    GuildWarLogRepository getGuildWarLogRepository();
}
