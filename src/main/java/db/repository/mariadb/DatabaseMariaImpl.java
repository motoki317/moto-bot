package db.repository.mariadb;

import db.ConnectionPool;
import db.Database;
import db.SimpleConnectionPool;
import db.repository.base.*;
import log.Logger;
import org.jetbrains.annotations.NotNull;

public class DatabaseMariaImpl implements Database {
    private static final String MYSQL_HOST = System.getenv("MYSQL_HOST");
    private static final String MYSQL_DATABASE = System.getenv("MYSQL_DATABASE");
    private static final String MYSQL_USER = System.getenv("MYSQL_USER");
    private static final String MYSQL_PASSWORD = System.getenv("MYSQL_PASSWORD");
    private static final int MYSQL_PORT = Integer.parseInt(System.getenv("MYSQL_PORT"));

    private static final String URL =  String.format(
            "jdbc:mariadb://%s:%s/%s?user=%s&password=%s",
            MYSQL_HOST, MYSQL_PORT, MYSQL_DATABASE, MYSQL_USER, MYSQL_PASSWORD);

    private final TrackChannelRepository trackChannelRepository;
    private final WorldRepository worldRepository;
    private final CommandLogRepository commandLogRepository;
    private final TerritoryRepository territoryRepository;
    private final TerritoryLogRepository territoryLogRepository;
    private final WarTrackRepository warTrackRepository;
    private final WarLogRepository warLogRepository;
    private final MariaWarPlayerRepository warPlayerRepository;
    private final TimeZoneRepository timeZoneRepository;
    private final PrefixRepository prefixRepository;
    private final GuildWarLogRepository guildWarLogRepository;
    private final DateFormatRepository dateFormatRepository;
    private final GuildRepository guildRepository;
    private final IgnoreChannelRepository ignoreChannelRepository;
    private final GuildLeaderboardRepository guildLeaderboardRepository;
    private final GuildXpLeaderboardRepository guildXpLeaderboardRepository;
    private final GuildWarLeaderboardRepository guildWarLeaderboardRepository;
    private final PlayerWarLeaderboardRepository playerWarLeaderboardRepository;
    private final TerritoryListRepository territoryListRepository;
    private final GuildListRepository guildListRepository;
    private final ServerLogRepository serverLogRepository;
    private final MusicSettingRepository musicSettingRepository;
    private final MusicQueueRepository musicQueueRepository;
    private final MusicInterruptedGuildRepository musicInterruptedGuildRepository;
    private final PlayerNumberRepository playerNumberRepository;

    @SuppressWarnings("OverlyCoupledMethod")
    public DatabaseMariaImpl(Logger logger) {
        ConnectionPool connectionPool = new SimpleConnectionPool(URL, logger, 10);
        this.trackChannelRepository = new MariaTrackChannelRepository(connectionPool, logger);
        this.worldRepository = new MariaWorldRepository(connectionPool, logger);
        this.commandLogRepository = new MariaCommandLogRepository(connectionPool, logger);
        this.territoryRepository = new MariaTerritoryRepository(connectionPool, logger);
        this.territoryLogRepository = new MariaTerritoryLogRepository(connectionPool, logger);
        this.warTrackRepository = new MariaWarTrackRepository(connectionPool, logger);
        this.warPlayerRepository = new MariaWarPlayerRepository(connectionPool, logger);
        this.warLogRepository = new MariaWarLogRepository(connectionPool, logger, warPlayerRepository);
        this.timeZoneRepository = new MariaTimeZoneRepository(connectionPool, logger);
        this.prefixRepository = new MariaPrefixRepository(connectionPool, logger);
        this.guildWarLogRepository = new MariaGuildWarLogRepository(connectionPool, logger);
        this.dateFormatRepository = new MariaDateFormatRepository(connectionPool, logger);
        this.guildRepository = new MariaGuildRepository(connectionPool, logger);
        this.ignoreChannelRepository = new MariaIgnoreChannelRepository(connectionPool, logger);
        this.guildLeaderboardRepository = new MariaGuildLeaderboardRepository(connectionPool, logger);
        this.guildXpLeaderboardRepository = new MariaGuildXpLeaderboardRepository(connectionPool, logger);
        this.guildWarLeaderboardRepository = new MariaGuildWarLeaderboardRepository(connectionPool, logger);
        this.playerWarLeaderboardRepository = new MariaPlayerWarLeaderboardRepository(connectionPool, logger);
        this.territoryListRepository = new MariaTerritoryListRepository(connectionPool, logger);
        this.guildListRepository = new MariaGuildListRepository(connectionPool, logger);
        this.serverLogRepository = new MariaServerLogRepository(connectionPool, logger);
        this.musicSettingRepository = new MariaMusicSettingRepository(connectionPool, logger);
        this.musicQueueRepository = new MariaMusicQueueRepository(connectionPool, logger);
        this.musicInterruptedGuildRepository = new MariaMusicInterruptedGuildRepository(connectionPool, logger);
        this.playerNumberRepository = new MariaPlayerNumberRepository(connectionPool, logger);
    }

    @Override
    public @NotNull TrackChannelRepository getTrackingChannelRepository() {
        return this.trackChannelRepository;
    }

    @Override
    public @NotNull WorldRepository getWorldRepository() {
        return this.worldRepository;
    }

    @Override
    public @NotNull CommandLogRepository getCommandLogRepository() {
        return this.commandLogRepository;
    }

    @Override
    public @NotNull TerritoryRepository getTerritoryRepository() {
        return this.territoryRepository;
    }

    @Override
    public @NotNull TerritoryLogRepository getTerritoryLogRepository() {
        return this.territoryLogRepository;
    }

    @Override
    public @NotNull WarTrackRepository getWarTrackRepository() {
        return this.warTrackRepository;
    }

    @Override
    public @NotNull WarLogRepository getWarLogRepository() {
        return this.warLogRepository;
    }

    @Override
    public @NotNull MariaWarPlayerRepository getWarPlayerRepository() {
        return this.warPlayerRepository;
    }

    @Override
    public @NotNull TimeZoneRepository getTimeZoneRepository() {
        return this.timeZoneRepository;
    }

    @Override
    public @NotNull PrefixRepository getPrefixRepository() {
        return this.prefixRepository;
    }

    @Override
    public @NotNull GuildWarLogRepository getGuildWarLogRepository() {
        return this.guildWarLogRepository;
    }

    @Override
    public @NotNull DateFormatRepository getDateFormatRepository() {
        return this.dateFormatRepository;
    }

    @Override
    public @NotNull GuildRepository getGuildRepository() {
        return this.guildRepository;
    }

    @Override
    public @NotNull IgnoreChannelRepository getIgnoreChannelRepository() {
        return this.ignoreChannelRepository;
    }

    @Override
    public @NotNull GuildLeaderboardRepository getGuildLeaderboardRepository() {
        return this.guildLeaderboardRepository;
    }

    @Override
    public @NotNull GuildXpLeaderboardRepository getGuildXpLeaderboardRepository() {
        return this.guildXpLeaderboardRepository;
    }

    @Override
    public @NotNull GuildWarLeaderboardRepository getGuildWarLeaderboardRepository() {
        return this.guildWarLeaderboardRepository;
    }

    @Override
    public @NotNull PlayerWarLeaderboardRepository getPlayerWarLeaderboardRepository() {
        return this.playerWarLeaderboardRepository;
    }

    @Override
    public @NotNull TerritoryListRepository getTerritoryListRepository() {
        return this.territoryListRepository;
    }

    @Override
    public @NotNull GuildListRepository getGuildListRepository() {
        return this.guildListRepository;
    }

    @Override
    public @NotNull ServerLogRepository getServerLogRepository() {
        return this.serverLogRepository;
    }

    @Override
    public @NotNull MusicSettingRepository getMusicSettingRepository() {
        return this.musicSettingRepository;
    }

    @Override
    public @NotNull MusicQueueRepository getMusicQueueRepository() {
        return this.musicQueueRepository;
    }

    @Override
    public @NotNull MusicInterruptedGuildRepository getMusicInterruptedGuildRepository() {
        return this.musicInterruptedGuildRepository;
    }

    @Override
    public @NotNull PlayerNumberRepository getPlayerNumberRepository() {
        return this.playerNumberRepository;
    }
}
