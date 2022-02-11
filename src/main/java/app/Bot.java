package app;

import db.Database;
import log.Logger;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.sharding.ShardManager;
import update.button.ButtonClickManager;

public interface Bot {
    ShardManager getManager();
    Properties getProperties();
    Database getDatabase();
    Logger getLogger();
    ButtonClickManager getButtonClickManager();

    int getShardId(JDA jda);
    void setConnected(int shardId, boolean connected);
    boolean isConnected(int shardId);
    boolean isAllConnected();
}
