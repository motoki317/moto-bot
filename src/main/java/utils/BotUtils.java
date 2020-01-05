package utils;

import net.dv8tion.jda.api.entities.TextChannel;

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

    /**
     * Sends long message (supports string longer than 2000 chars) to text channel.
     * Splits the message into parts to send to discord.
     * @param message Message to send.
     * @param ch Text channel to post on.
     */
    public static void sendLongMessage(String message, TextChannel ch) {
        double length = message.length();
        for (int i = 0; i < Math.ceil(length / 2000D); i++) {
            int start = i * 2000;
            int end = Math.min((i + 1) * 2000, (int) length);
            ch.sendMessage(message.substring(start, end)).queue();
        }
    }
}
