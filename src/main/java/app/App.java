package app;

import listeners.MessageListener;
import log.ConsoleLogger;
import log.DiscordLogger;
import model.BotData;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.SessionController;
import net.dv8tion.jda.api.utils.SessionControllerAdapter;
import log.Logger;

import javax.annotation.Nullable;
import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;

public class App implements Runnable, Bot {
    private final ShardManager manager;

    private final Properties properties;

    private final DataLoader dataLoader;

    private Logger logger;

    private boolean[] isConnected;

    @Override
    public ShardManager getManager() {
        return this.manager;
    }

    @Override
    public Properties getProperties() {
        return this.properties;
    }

    @Override
    public BotData getBotData() {
        return this.dataLoader.getData();
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

        DataLoader dataLoader = this.loadBotData();
        if (dataLoader == null) {
            System.exit(1);
        }
        this.dataLoader = dataLoader;

        this.waitJDALoading();

        this.addEventListeners();

        this.setShutDownHook();

        this.manager.setActivity(Activity.playing("Bot load complete!"));
        this.logger.log(-1, "Bot load complete!");

        this.logger = new DiscordLogger(this, this.properties.logTimeZone);

        this.logger.log(0, "Bot is ready!");
    }

    public void run() {
        // Run Heartbeat, etc.
    }

    /**
     * Tries to load bot data and returns data loader instance. Blocking.
     * @return Data loader.
     */
    @Nullable
    private DataLoader loadBotData() {
        try {
            return new DataLoader();
        } catch (IOException e) {
            this.logger.logError("an error occurred while loading bot data", e);
            return null;
        }
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
        manager.addEventListener(new MessageListener(this));
        this.logger.log(-1, "Added event listeners.");
    }

    private void setShutDownHook() {
        App app = this;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            app.logger.log(0, "Bot shutting down...");
            app.logger = new ConsoleLogger(app.properties.logTimeZone);
            try {
                app.dataLoader.saveData();
                app.logger.log(0, "Successfully saved bot data locally.");
            } catch (IOException e) {
                app.logger.logError("an error occurred while saving bot data", e);
            }
        }));
    }
}
