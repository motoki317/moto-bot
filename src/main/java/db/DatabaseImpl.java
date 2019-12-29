package db;

import db.repository.*;
import log.Logger;
import org.jetbrains.annotations.NotNull;

public class DatabaseImpl implements Database {
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
    private WarPlayerRepository warPlayerRepository;
    private CustomTimeZoneRepository customTimeZoneRepository;
    private PrefixRepository prefixRepository;

    public DatabaseImpl(Logger logger) {
        this.logger = logger;
        this.connectionPool = new SimpleConnectionPool(URL, logger, 10);
    }

    @Override
    @NotNull
    public TrackChannelRepository getTrackingChannelRepository() {
        if (this.trackChannelRepository == null) {
            this.trackChannelRepository = new TrackChannelRepository(this.connectionPool, this.logger);
        }
        return this.trackChannelRepository;
    }

    @Override
    @NotNull
    public WorldRepository getWorldRepository() {
        if (this.worldRepository == null) {
            this.worldRepository = new WorldRepository(this.connectionPool, this.logger);
        }
        return this.worldRepository;
    }

    @Override
    @NotNull
    public CommandLogRepository getCommandLogRepository() {
        if (this.commandLogRepository == null) {
            this.commandLogRepository = new CommandLogRepository(this.connectionPool, this.logger);
        }
        return this.commandLogRepository;
    }

    @Override
    public @NotNull TerritoryRepository getTerritoryRepository() {
        if (this.territoryRepository == null) {
            this.territoryRepository = new TerritoryRepository(this.connectionPool, this.logger);
        }
        return this.territoryRepository;
    }

    @Override
    public @NotNull TerritoryLogRepository getTerritoryLogRepository() {
        if (this.territoryLogRepository == null) {
            this.territoryLogRepository = new TerritoryLogRepository(this.connectionPool, this.logger);
        }
        return this.territoryLogRepository;
    }

    @Override
    public @NotNull WarTrackRepository getWarTrackRepository() {
        if (this.warTrackRepository == null) {
            this.warTrackRepository = new WarTrackRepository(this.connectionPool, this.logger);
        }
        return this.warTrackRepository;
    }

    @Override
    public @NotNull WarLogRepository getWarLogRepository() {
        if (this.warLogRepository == null) {
            this.warLogRepository = new WarLogRepository(this.connectionPool, this.logger, this.getWarPlayerRepository());
        }
        return this.warLogRepository;
    }

    @Override
    public @NotNull WarPlayerRepository getWarPlayerRepository() {
        if (this.warPlayerRepository == null) {
            this.warPlayerRepository = new WarPlayerRepository(this.connectionPool, this.logger);
        }
        return this.warPlayerRepository;
    }

    @Override
    public @NotNull CustomTimeZoneRepository getCustomTimeZoneRepository() {
        if (this.customTimeZoneRepository == null) {
            this.customTimeZoneRepository = new CustomTimeZoneRepository(this.connectionPool, this.logger);
        }
        return this.customTimeZoneRepository;
    }

    @Override
    public @NotNull PrefixRepository getPrefixRepository() {
        if (this.prefixRepository == null) {
            this.prefixRepository = new PrefixRepository(this.connectionPool, this.logger);
        }
        return this.prefixRepository;
    }
}
