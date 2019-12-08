package app;

import listeners.MessageListener;
import log.ConsoleLogger;
import log.DiscordLogger;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.SessionController;
import net.dv8tion.jda.api.utils.SessionControllerAdapter;
import log.Logger;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;

public class App implements Runnable, Bot {
    private final ShardManager manager;

    private final Properties properties;

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

        for (int i = 0; i < shardsTotal; i++) {
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

        this.addEventListeners();

        this.manager.setActivity(Activity.playing("Bot load complete!"));
        this.logger.log(-1, "Bot load complete!");

        this.logger = new DiscordLogger(this, this.properties.logTimeZone);

        this.logger.log(0, "Bot is ready!");
    }

    public void run() {
        // Run Heartbeat, etc.
    }

    private void addEventListeners() {
        manager.addEventListener(new MessageListener(this));
        this.logger.log(-1, "Added event listeners.");
    }
}