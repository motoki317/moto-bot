package app;

import db.Database;
import db.model.world.World;
import db.repository.mariadb.DatabaseMariaImpl;
import heartbeat.HeartBeat;
import log.ConsoleLogger;
import log.DiscordLogger;
import log.Logger;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.SessionControllerAdapter;
import update.UpdaterFactory;
import update.button.ButtonClickManager;
import utils.FormatUtils;
import utils.StoppableThread;

import javax.security.auth.login.LoginException;
import java.util.Date;
import java.util.List;

public class App implements Runnable, Bot {
    private final ShardManager manager;

    private final Properties properties;

    private final Database database;

    private Logger logger;

    private final ButtonClickManager buttonClickManager;

    private final StoppableThread heartBeat;

    private final boolean[] connected;

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
    public ButtonClickManager getButtonClickManager() {
        return this.buttonClickManager;
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
    public void setConnected(int shardId, boolean connected) {
        if (shardId < 0 || this.connected.length <= shardId) return;
        boolean prev;
        synchronized (this.connected) {
            prev = this.connected[shardId];
            this.connected[shardId] = connected;
        }
        if (prev != connected) {
            if (connected) {
                this.logger.log(-1, "Shard " + shardId + " went online");
            } else {
                this.logger.log(-1, "Shard " + shardId + " went offline");
            }
        }
    }

    @Override
    public boolean isConnected(int shardId) {
        if (shardId < 0 || this.connected.length <= shardId) return false;
        synchronized (this.connected) {
            return this.connected[shardId];
        }
    }

    @Override
    public boolean isAllConnected() {
        synchronized (this.connected) {
            for (boolean c : this.connected) {
                if (!c) return false;
            }
            return true;
        }
    }

    public App(Properties properties, UpdaterFactory updaterFactory) throws LoginException {
        this.properties = properties;
        this.logger = new ConsoleLogger(this.properties.logTimeZone);
        this.buttonClickManager = updaterFactory.getButtonClickManager();
        this.connected = new boolean[this.properties.shards];

        this.manager = DefaultShardManagerBuilder.createDefault(this.properties.botAccessToken)
                // For server log feature
                .enableIntents(GatewayIntent.GUILD_MEMBERS)
                .setSessionController(new SessionControllerAdapter())
                .setShardsTotal(this.properties.shards)
                .setShards(0, this.properties.shards - 1)
                .build();

        this.manager.setActivity(Activity.playing("Bot restarting..."));

        this.waitJDALoading();

        this.manager.setActivity(Activity.playing("Bot load complete!"));
        this.logger.log(-1, "Bot load complete!");

        // Wait JDA to be fully loaded before instantiating discord logger
        this.logger = new DiscordLogger(this, this.properties.logTimeZone);

        this.database = new DatabaseMariaImpl(this.logger);

        this.heartBeat = new HeartBeat(this);
        this.heartBeat.setName("moto-bot heartbeat");

        this.sendReadyMessage();

        this.addEventListeners();
    }

    public void run() {
        this.heartBeat.start();
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
            this.setConnected(i, true);
        }
        this.logger.log(-1, "JDA Sharding: All shards loaded!");
    }

    private void sendReadyMessage() {
        List<World> worlds = this.database.getWorldRepository().findAll();
        String downtime;
        if (worlds == null || worlds.size() == 0) {
            downtime = "(Failed to retrieve downtime)";
        } else {
            Date lastPlayerTracker = worlds.get(0).getUpdatedAt();
            long seconds = (new Date().getTime() - lastPlayerTracker.getTime()) / 1000L;
            downtime = FormatUtils.formatReadableTime(seconds, false, "s");
        }

        String readyMessage = String.format(
                "Bot is ready!\n" +
                        "Downtime: %s",
                downtime
        );
        this.logger.log(0, readyMessage);
        TextChannel restartCh = this.manager.getTextChannelById(this.properties.botRestartChannelId);
        if (restartCh == null) {
            this.logger.log(0, "Failed to get restart text channel");
            return;
        }
        restartCh.sendMessage(readyMessage).queue();
    }

    private void addEventListeners() {
        this.manager.addEventListener(new CommandListener(this));
        this.manager.addEventListener(new UpdaterListener(this));
        this.manager.addEventListener(new ServerLogListener(this));
        this.logger.debug("Added event listeners.");
    }

    public void onShutDown() {
        this.logger.log(0, "Bot shutting down...");
        this.logger = new ConsoleLogger(this.properties.logTimeZone);
        this.heartBeat.terminate();
    }
}
