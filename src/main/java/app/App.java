package app;

import listeners.MessageListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.SessionController;
import net.dv8tion.jda.api.utils.SessionControllerAdapter;
import utils.Logger;

import javax.security.auth.login.LoginException;
import java.util.Arrays;

public class App implements Runnable {
    public static ShardManager MANAGER;

    public static Properties BOT_PROPERTIES;

    private static boolean[] isConnected;

    public static boolean isConnected(int shardId) {
        return isConnected[shardId];
    }

    public static boolean isAllConnected() {
        for (boolean shardConnected : isConnected) {
            if (!shardConnected) return false;
        }
        return true;
    }

    public static void setAllConnected(boolean newState) {
        Arrays.fill(isConnected, newState);
    }

    static {
        try {
            BOT_PROPERTIES = new Properties();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        isConnected = new boolean[BOT_PROPERTIES.shards];
    }

    public App() {
        this.init();
        this.addEventListeners();

        MANAGER.setActivity(Activity.playing("Bot load complete!"));
        Logger.log(-1, "Bot load complete!");
    }

    public void run() {
        // Run Heartbeat, etc.
    }

    private void init() {
        DefaultShardManagerBuilder builder = new DefaultShardManagerBuilder();
        SessionController sessionController = new SessionControllerAdapter();

        builder.setToken(BOT_PROPERTIES.botAccessToken);
        builder.setSessionController(sessionController);
        int shardsTotal = BOT_PROPERTIES.shards;
        builder.setShardsTotal(shardsTotal);
        builder.setShards(0, shardsTotal - 1);

        try {
            MANAGER = builder.build();
        } catch (LoginException e) {
            e.printStackTrace();
            System.exit(1);
        }
        MANAGER.setActivity(Activity.playing("Bot restarting..."));

        for (int i = 0; i < shardsTotal; i++) {
            while (MANAGER.getStatus(i) != JDA.Status.CONNECTED) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            isConnected[i] = true;
            Logger.log(-1, "JDA Sharding: Shard ID " + i + " is loaded!");
        }
        Logger.log(-1, "JDA Sharding: All shards loaded!");
    }

    private void addEventListeners() {
        MANAGER.addEventListener(new MessageListener());
        Logger.log(-1, "Added event listeners.");
    }
}
