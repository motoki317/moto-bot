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

    private final Logger logger;

    private final ConnectionPool connectionPool;

    // Repository instance cache
    private TrackChannelRepository trackChannelRepository;
    private WorldRepository worldRepository;
    private CommandLogRepository commandLogRepository;
    private TerritoryRepository territoryRepository;
    private TerritoryLogRepository territoryLogRepository;
    private WarTrackRepository warTrackRepository;
    private WarLogRepository warLogRepository;
    private MariaWarPlayerRepository warPlayerRepository;
    private TimeZoneRepository timeZoneRepository;
    private PrefixRepository prefixRepository;
    private GuildWarLogRepository guildWarLogRepository;
    private DateFormatRepository dateFormatRepository;
    private GuildRepository guildRepository;
    private IgnoreChannelRepository ignoreChannelRepository;
    private GuildLeaderboardRepository guildLeaderboardRepository;
    private GuildXpLeaderboardRepository guildXpLeaderboardRepository;
    private GuildWarLeaderboardRepository guildWarLeaderboardRepository;
    private PlayerWarLeaderboardRepository playerWarLeaderboardRepository;
    private TerritoryListRepository territoryListRepository;
    private GuildListRepository guildListRepository;
    private ServerLogRepository serverLogRepository;
    private MusicSettingRepository musicSettingRepository;
    private MusicQueueRepository musicQueueRepository;

    public DatabaseMariaImpl(Logger logger) {
        this.logger = logger;
        this.connectionPool = new SimpleConnectionPool(URL, logger, 10);
    }

    @Override
    @NotNull
    public TrackChannelRepository getTrackingChannelRepository() {
        if (this.trackChannelRepository == null) {
            this.trackChannelRepository = new MariaTrackChannelRepository(this.connectionPool, this.logger);
        }
        return this.trackChannelRepository;
    }

    @Override
    @NotNull
    public WorldRepository getWorldRepository() {
        if (this.worldRepository == null) {
            this.worldRepository = new MariaWorldRepository(this.connectionPool, this.logger);
        }
        return this.worldRepository;
    }

    @Override
    @NotNull
    public CommandLogRepository getCommandLogRepository() {
        if (this.commandLogRepository == null) {
            this.commandLogRepository = new MariaCommandLogRepository(this.connectionPool, this.logger);
        }
        return this.commandLogRepository;
    }

    @Override
    public @NotNull TerritoryRepository getTerritoryRepository() {
        if (this.territoryRepository == null) {
            this.territoryRepository = new MariaTerritoryRepository(this.connectionPool, this.logger);
        }
        return this.territoryRepository;
    }

    @Override
    public @NotNull TerritoryLogRepository getTerritoryLogRepository() {
        if (this.territoryLogRepository == null) {
            this.territoryLogRepository = new MariaTerritoryLogRepository(this.connectionPool, this.logger);
        }
        return this.territoryLogRepository;
    }

    @Override
    public @NotNull WarTrackRepository getWarTrackRepository() {
        if (this.warTrackRepository == null) {
            this.warTrackRepository = new MariaWarTrackRepository(this.connectionPool, this.logger);
        }
        return this.warTrackRepository;
    }

    @Override
    public @NotNull WarLogRepository getWarLogRepository() {
        if (this.warLogRepository == null) {
            this.warLogRepository = new MariaWarLogRepository(this.connectionPool, this.logger, this.getWarPlayerRepository());
        }
        return this.warLogRepository;
    }

    @Override
    public @NotNull MariaWarPlayerRepository getWarPlayerRepository() {
        if (this.warPlayerRepository == null) {
            this.warPlayerRepository = new MariaWarPlayerRepository(this.connectionPool, this.logger);
        }
        return this.warPlayerRepository;
    }

    @Override
    public @NotNull TimeZoneRepository getTimeZoneRepository() {
        if (this.timeZoneRepository == null) {
            this.timeZoneRepository = new MariaTimeZoneRepository(this.connectionPool, this.logger);
        }
        return this.timeZoneRepository;
    }

    @Override
    public @NotNull PrefixRepository getPrefixRepository() {
        if (this.prefixRepository == null) {
            this.prefixRepository = new MariaPrefixRepository(this.connectionPool, this.logger);
        }
        return this.prefixRepository;
    }

    @Override
    public @NotNull GuildWarLogRepository getGuildWarLogRepository() {
        if (this.guildWarLogRepository == null) {
            this.guildWarLogRepository = new MariaGuildWarLogRepository(this.connectionPool, this.logger);
        }
        return this.guildWarLogRepository;
    }

    @Override
    public @NotNull DateFormatRepository getDateFormatRepository() {
        if (this.dateFormatRepository == null) {
            this.dateFormatRepository = new MariaDateFormatRepository(this.connectionPool, this.logger);
        }
        return this.dateFormatRepository;
    }

    @Override
    public @NotNull GuildRepository getGuildRepository() {
        if (this.guildRepository == null) {
            this.guildRepository = new MariaGuildRepository(this.connectionPool, this.logger);
        }
        return this.guildRepository;
    }

    @Override
    public @NotNull IgnoreChannelRepository getIgnoreChannelRepository() {
        if (this.ignoreChannelRepository == null) {
            this.ignoreChannelRepository = new MariaIgnoreChannelRepository(this.connectionPool, this.logger);
        }
        return this.ignoreChannelRepository;
    }

    @Override
    public @NotNull GuildLeaderboardRepository getGuildLeaderboardRepository() {
        if (this.guildLeaderboardRepository == null) {
            this.guildLeaderboardRepository = new MariaGuildLeaderboardRepository(this.connectionPool, this.logger);
        }
        return this.guildLeaderboardRepository;
    }

    @Override
    public @NotNull GuildXpLeaderboardRepository getGuildXpLeaderboardRepository() {
        if (this.guildXpLeaderboardRepository == null) {
            this.guildXpLeaderboardRepository = new MariaGuildXpLeaderboardRepository(this.connectionPool, this.logger);
        }
        return this.guildXpLeaderboardRepository;
    }

    @Override
    public @NotNull GuildWarLeaderboardRepository getGuildWarLeaderboardRepository() {
        if (this.guildWarLeaderboardRepository == null) {
            this.guildWarLeaderboardRepository = new MariaGuildWarLeaderboardRepository(this.connectionPool, this.logger);
        }
        return this.guildWarLeaderboardRepository;
    }

    @Override
    public @NotNull PlayerWarLeaderboardRepository getPlayerWarLeaderboardRepository() {
        if (this.playerWarLeaderboardRepository == null) {
            this.playerWarLeaderboardRepository = new MariaPlayerWarLeaderboardRepository(this.connectionPool, this.logger);
        }
        return this.playerWarLeaderboardRepository;
    }

    @Override
    public @NotNull TerritoryListRepository getTerritoryListRepository() {
        if (this.territoryListRepository == null) {
            this.territoryListRepository = new MariaTerritoryListRepository(this.connectionPool, this.logger);
        }
        return this.territoryListRepository;
    }

    @Override
    public @NotNull GuildListRepository getGuildListRepository() {
        if (this.guildListRepository == null) {
            this.guildListRepository = new MariaGuildListRepository(this.connectionPool, this.logger);
        }
        return this.guildListRepository;
    }

    @Override
    public @NotNull ServerLogRepository getServerLogRepository() {
        if (this.serverLogRepository == null) {
            this.serverLogRepository = new MariaServerLogRepository(this.connectionPool, this.logger);
        }
        return this.serverLogRepository;
    }

    @Override
    public @NotNull MusicSettingRepository getMusicSettingRepository() {
        if (this.musicSettingRepository == null) {
            this.musicSettingRepository = new MariaMusicSettingRepository(this.connectionPool, this.logger);
        }
        return this.musicSettingRepository;
    }

    @Override
    public @NotNull MusicQueueRepository getMusicQueueRepository() {
        if (this.musicQueueRepository == null) {
            this.musicQueueRepository = new MariaMusicQueueRepository(this.connectionPool, this.logger);
        }
        return this.musicQueueRepository;
    }
}
