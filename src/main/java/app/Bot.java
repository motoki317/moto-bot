package app;

import db.model.BotData;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.sharding.ShardManager;
import log.Logger;

public interface Bot {
    ShardManager getManager();
    Properties getProperties();
    BotData getBotData();
    Logger getLogger();
    int getShardId(JDA jda);
    boolean isConnected(int shardId);
    boolean isAllConnected();
}
