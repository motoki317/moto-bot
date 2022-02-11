package log;

import app.Bot;
import commands.event.CommandEvent;
import net.dv8tion.jda.api.entities.TextChannel;
import org.slf4j.LoggerFactory;
import utils.FormatUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.RejectedExecutionException;
import java.util.stream.Collectors;

/**
 * DiscordLogger implements Logger, discord channel logging, and checks message spams.
 */
public class DiscordLogger implements Logger {
    private final Bot bot;

    private final Map<Integer, Long> logChannels;

    private final DateFormat logFormat;

    private final org.slf4j.Logger logger;

    public DiscordLogger(Bot bot, TimeZone logTimeZone) {
        this.bot = bot;
        // deep copy
        this.logChannels = bot.getProperties().logChannelId
                .entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        this.logFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
        this.logFormat.setTimeZone(logTimeZone);
        this.logger = LoggerFactory.getLogger(DiscordLogger.class);
    }

    /**
     * Appends time to the given message and logs to discord channels.
     * Also prints to the std out.
     *
     * @param botLogCh Channel to log.
     * @param message  Message to log.
     */
    public void log(int botLogCh, CharSequence message) {
        Date now = new Date();
        String msgTimeAppended = this.logFormat.format(now) + " " + message;

        // To standard out
        this.logger.info(message.toString());
        // To Discord channel
        this.logToDiscord(botLogCh, msgTimeAppended);
    }

    /**
     * Logs to discord channel.
     *
     * @param botLogCh Channel to log.
     * @param message  Raw message.
     */
    private void logToDiscord(int botLogCh, String message) {
        if (!this.logChannels.containsKey(botLogCh)) {
            return;
        }
        long logChannelId = this.logChannels.get(botLogCh);
        TextChannel ch = this.bot.getManager().getTextChannelById(logChannelId);
        if (ch == null) {
            return;
        }

        sendLongMessage(message, ch);
    }

    /**
     * Sends long message (supports string longer than 2000 chars) to text channel.
     * Splits the message into parts to send to discord.
     *
     * @param message Message to send.
     * @param ch      Text channel to post on.
     */
    private void sendLongMessage(String message, TextChannel ch) {
        double length = message.length();
        for (int i = 0; i < Math.ceil(length / 2000D); i++) {
            int start = i * 2000;
            int end = Math.min((i + 1) * 2000, (int) length);
            try {
                ch.sendMessage(message.substring(start, end)).queue();
            } catch (RejectedExecutionException e) {
                // Expected to be thrown on JDA shutdown
                this.debug("Logger: Failed to send message:\n" + e.getMessage());
            }
        }
    }

    @Override
    public void debug(CharSequence message) {
        this.logger.debug(message.toString());
    }

    /**
     * Logs message received event to bot-log-4.
     *
     * @param event Guild message received event.
     */
    public void logEvent(CommandEvent event, boolean isSpam) {
        String logMsg = createCommandLog(event, isSpam);
        this.log(4, logMsg);
    }

    /**
     * Create a user command log String. <br/>
     * Example output <br/>
     * 2018/10/01 11:02:46.430 (Guild)[Channel]&lt;User&gt;: `>ping` <br/>
     * 2019/12/04 19:12:06.334 [DM UserName#1234]&lt;User&gt;: `>help`
     *
     * @param event Guild message received event.
     * @return Human-readable user command usage log.
     */
    static String createCommandLog(CommandEvent event, boolean isSpam) {
        if (event.isFromGuild()) {
            return String.format(
                    "(%s)[%s]<%s>: `%s`%s",
                    event.getGuild().getName(),
                    event.getChannel().getName(),
                    event.getAuthor().getName(),
                    event.getContentRaw(),
                    isSpam ? " Spam detected" : ""
            );
        } else {
            return String.format(
                    "[DM %s]<%s>: `%s`%s",
                    FormatUtils.getUserFullName(event.getAuthor()),
                    event.getAuthor().getName(),
                    event.getContentRaw(),
                    isSpam ? " Spam detected" : ""
            );
        }
    }

    @Override
    public void logException(CharSequence message, Throwable e) {
        Date now = new Date();
        String msgTimeAppended = String.format(
                "%s %s\n%s",
                this.logFormat.format(now), message, e.toString()
        );
        // Print short version to Discord channel 0
        this.logToDiscord(0, msgTimeAppended);
        // Full log to logger
        this.logger.warn("Exception caught", e);
    }
}
