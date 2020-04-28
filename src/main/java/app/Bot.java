package app;

import db.Database;
import log.Logger;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.sharding.ShardManager;
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
}
