package db;

import db.repository.CommandLogRepository;
import db.repository.TerritoryRepository;
import db.repository.TrackChannelRepository;
import db.repository.WorldRepository;
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
}
