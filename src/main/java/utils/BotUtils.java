package utils;

import app.Bot;
import net.dv8tion.jda.api.JDA;

import java.util.List;

public class BotUtils {
    /**
     * Get shard id of the given jda instance.
     * @param bot Bot instance.
     * @param jda JDA instance.
     * @return Shard id. Returns -1 if not found.
     */
    public static int getShardId(Bot bot, JDA jda) {
        List<JDA> shards = bot.getManager().getShards();
        for (int i = 0; i < shards.size(); i++) {
            JDA shard = shards.get(i);
            if (shard.equals(jda)) {
                return i;
            }
        }
        return -1;
    }
}
