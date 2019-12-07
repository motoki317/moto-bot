package log;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class DiscordLogger implements Logger {
    private final DateFormat logFormat;

    private final DiscordSpamChecker spamChecker;

    public DiscordLogger(TimeZone logTimeZone) {
        this.logFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
        this.logFormat.setTimeZone(logTimeZone);
        this.spamChecker = new DiscordSpamChecker();
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
        boolean isSpam = this.spamChecker.isSpam(event);
        String logMsg = this.createCommandLog(event, isSpam);
        this.log(4, logMsg);
        return isSpam;
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
