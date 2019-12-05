package log;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import utils.BotUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class DiscordLogger implements Logger {
    // 1 second
    private static final long SPAM_PREVENTION = TimeUnit.SECONDS.toMillis(1);

    private static final long CACHE_TIME = TimeUnit.MINUTES.toMillis(1);

    private final DateFormat logFormat;

    // User ID to last message time
    private final Map<Long, Long> messages;

    public DiscordLogger(TimeZone logTimeZone) {
        this.logFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
        this.logFormat.setTimeZone(logTimeZone);
        this.messages = new HashMap<>();
    }

    /**
     * Logs normal message.
     * @param botLogCh Channel to log.
     * @param message Message to log.
     */
    public void log(int botLogCh, CharSequence message) {
        Date now = new Date();
        String msg = this.logFormat.format(now) + " " + message;

        System.out.println(msg);

        // Log to discord channels
    }

    /**
     * Logs message received event to bot-log-4.
     * @param event Guild message received event.
     */
    public boolean logEvent(MessageReceivedEvent event) {
        boolean isSpam = isSpamMessage(event);
        String logMsg = this.createCommandLog(event, isSpam);
        this.log(4, logMsg);
        return isSpam;
    }

    /**
     * Determines if the received event is a spam from the same user.
     * @param event Message received event.
     * @return True if this is a spam.
     */
    private boolean isSpamMessage(MessageReceivedEvent event) {
        long userId = event.getAuthor().getIdLong();
        long time = BotUtils.getIdCreationTime(event.getMessageIdLong());
        long lastMessageTime = this.messages.getOrDefault(userId, -1L);

        if (lastMessageTime == -1L) {
            this.messages.put(userId, time);
            return false;
        }

        long diff = time - lastMessageTime;
        if (diff < SPAM_PREVENTION) {
            return true;
        }
        this.messages.put(userId, lastMessageTime);

        this.removeOldMessageCache(time);
        return false;
    }

    private void removeOldMessageCache(long currentTime) {
        this.messages.entrySet().removeIf((e) -> CACHE_TIME < (currentTime - e.getValue()));
    }

    /**
     * Create a user command log String. <br/>
     * Example output <br/>
     * 2018/10/01 11:02:46.430 (Guild)[Channel]&lt;User&gt;: `>ping` <br/>
     * 2019/12/04 19:12:06.334 [DM UserName#1234]&lt;User&gt;: `>help`
     * @param event Guild message received event.
     * @return Human readable user command usage log.
     */
    private String createCommandLog(MessageReceivedEvent event, boolean isSpam) {
        Date now = new Date();

        if (event.isFromGuild()) {
            return this.logFormat.format(now) + " (" + event.getGuild().getName() + ")[" +
                    event.getChannel().getName() + "]<" + event.getAuthor().getName() + ">: `" +
                    event.getMessage().getContentRaw() + "`" + (isSpam ? " Spam detected" : "");
        } else {
            return this.logFormat.format(now) + " [DM " + getFullUserName(event.getAuthor()) + "]<" +
                    event.getAuthor().getName() + ">: `" +
                    event.getMessage().getContentRaw() + "`" + (isSpam ? " Spam detected" : "");
        }
    }

    private static String getFullUserName(User user) {
        return user.getName() + "#" + user.getDiscriminator();
    }

}
