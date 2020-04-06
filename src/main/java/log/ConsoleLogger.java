package log;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import static log.DiscordLogger.createCommandLog;

/**
 * ConsoleLogger only logs to standard output.
 */
public class ConsoleLogger implements Logger {
    private final DateFormat logFormat;
    private final boolean debug;

    public ConsoleLogger(TimeZone logTimeZone) {
        this.logFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
        this.logFormat.setTimeZone(logTimeZone);
        this.debug = "1".equals(System.getenv("DEBUG"));
    }

    @Override
    public void log(int botLogCh, CharSequence message) {
        Date now = new Date();
        String msg = this.logFormat.format(now) + " " + message;

        System.out.println(msg);
    }

    @Override
    public void debug(CharSequence message) {
        if (!debug) return;
        Date now = new Date();
        String msg = this.logFormat.format(now) + " " + message;
        System.out.println(msg);
    }

    @Override
    public void logEvent(MessageReceivedEvent event, boolean isSpam) {
        String logMsg = createCommandLog(event, isSpam);
        this.log(4, logMsg);
    }

    @Override
    public void logException(CharSequence message, Throwable e) {
        this.log(0, message);
        e.printStackTrace();
    }
}
