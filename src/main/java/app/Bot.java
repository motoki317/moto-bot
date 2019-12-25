package app;

import db.Database;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.sharding.ShardManager;
import log.Logger;
import update.reaction.ReactionManager;
import update.response.ResponseManager;

public interface Bot {
    ShardManager getManager();
    Properties getProperties();
    Database getDatabase();
    Logger getLogger();
    ReactionManager getReactionManager();
    ResponseManager getResponseManager();
    int getShardId(JDA jda);
    boolean isConnected(int shardId);
    boolean isAllConnected();
}
