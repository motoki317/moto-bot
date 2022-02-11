package log;

import commands.event.CommandEvent;
import org.slf4j.LoggerFactory;

import static log.DiscordLogger.createCommandLog;

/**
 * ConsoleLogger only logs to standard output.
 */
public class ConsoleLogger implements Logger {
    private final org.slf4j.Logger logger;

    public ConsoleLogger() {
        this.logger = LoggerFactory.getLogger(ConsoleLogger.class);
    }

    @Override
    public void log(int botLogCh, CharSequence message) {
        this.logger.info(message.toString());
    }

    @Override
    public void debug(CharSequence message) {
        this.log(-1, message);
    }

    @Override
    public void logEvent(CommandEvent event, boolean isSpam) {
        String logMsg = createCommandLog(event, isSpam);
        this.log(4, logMsg);
    }

    @Override
    public void logException(CharSequence message, Throwable e) {
        this.logger.warn("Exception caught", e);
    }
}
