package app;

import db.Database;
import db.DatabaseConnection;
import heartbeat.HeartBeat;
import listeners.MessageListener;
import log.ConsoleLogger;
import log.DiscordLogger;
import log.Logger;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.SessionController;
import net.dv8tion.jda.api.utils.SessionControllerAdapter;
import utils.StoppableThread;

import javax.annotation.Nullable;
import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.List;

public class App implements Runnable, Bot {
    private final ShardManager manager;

    private final Properties properties;

    private final Database database;

    private Logger logger;

    private boolean[] isConnected;

    private final StoppableThread heartBeat;

    @Override
    public ShardManager getManager() {
        return this.manager;
    }

    @Override
    public Properties getProperties() {
        return this.properties;
    }

    @Override
    public Database getDatabase() {
        return this.database;
    }

    @Override
    public Logger getLogger() {
        return this.logger;
    }

    @Override
    public int getShardId(JDA jda) {
        List<JDA> shards = this.manager.getShards();
        for (int i = 0; i < shards.size(); i++) {
            JDA shard = shards.get(i);
            if (shard.equals(jda)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean isConnected(int shardId) {
        return this.isConnected[shardId];
    }

    @Override
    public boolean isAllConnected() {
        for (boolean shardConnected : this.isConnected) {
            if (!shardConnected) return false;
        }
        return true;
    }

    public App() throws IOException, ParseException, LoginException {
        this.properties = new Properties();
        this.logger = new ConsoleLogger(this.properties.logTimeZone);
        this.isConnected = new boolean[this.properties.shards];

        DefaultShardManagerBuilder builder = new DefaultShardManagerBuilder();
        SessionController sessionController = new SessionControllerAdapter();

        builder.setToken(this.properties.botAccessToken);
        builder.setSessionController(sessionController);
        int shardsTotal = this.properties.shards;
        builder.setShardsTotal(shardsTotal);
        builder.setShards(0, shardsTotal - 1);

        this.manager = builder.build();
        manager.setActivity(Activity.playing("Bot restarting..."));

        Database database = connectDatabase(this.logger);
        if (database == null) {
            throw new Error("Failed to initialize database");
        }
        this.database = database;
        this.logger.log(-1, "Successfully established connection to db!");

        this.waitJDALoading();

        this.heartBeat = new HeartBeat(this);
        this.heartBeat.setName("moto-bot heartbeat");

        this.manager.setActivity(Activity.playing("Bot load complete!"));
        this.logger.log(-1, "Bot load complete!");

        this.logger = new DiscordLogger(this, this.properties.logTimeZone);

        this.logger.log(0, "Bot is ready!");

        this.setShutDownHook();
        this.addEventListeners();
    }

    public void run() {
        this.heartBeat.start();
    }

    /**
     * Tries to establish connection to and initialize database.
     * @return Database.
     */
    @Nullable
    private static Database connectDatabase(Logger logger) {
        try {
            return new DatabaseConnection(logger);
        } catch (SQLException e) {
            logger.logException("an exception occurred while establishing connection to db", e);
        }
        return null;
    }

    /**
     * Waits and blocks this thread until all JDA shards are loaded.
     */
    private void waitJDALoading() {
        for (int i = 0; i < this.properties.shards; i++) {
            while (manager.getStatus(i) != JDA.Status.CONNECTED) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            isConnected[i] = true;
            this.logger.log(-1, "JDA Sharding: Shard ID " + i + " is loaded!");
        }
        this.logger.log(-1, "JDA Sharding: All shards loaded!");
    }

    private void addEventListeners() {
        this.manager.addEventListener(new MessageListener(this));
        this.logger.log(-1, "Added event listeners.");
    }

    private void setShutDownHook() {
        App app = this;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            app.logger.log(0, "Bot shutting down...");
            app.logger = new ConsoleLogger(app.properties.logTimeZone);
            app.heartBeat.terminate();
        }));
    }
}
