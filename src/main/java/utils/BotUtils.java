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

    /**
     * Get a creation time in long (epoch, milliseconds from 1970/01/01 00:00:00.000 UTC) from discord IDs such as
     *  user / channel / guild / message IDs. <br/>
     * Discord epoch (first second of 2014): 1420070400000 (from discordapi.com) <br/>
     * Returns (discordId >> 22) + 1420070400000L.
     * @param discordId Discord ID.
     * @return Time in epoch milliseconds.
     */
    public static long getIdCreationTime(long discordId) {
        return (discordId >> 22) + 1420070400000L;
    }
}
