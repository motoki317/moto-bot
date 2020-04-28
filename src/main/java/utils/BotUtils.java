package utils;

public class BotUtils {
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
