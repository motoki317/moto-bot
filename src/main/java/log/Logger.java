package log;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public interface Logger {
    void log(int botLogCh, CharSequence message);
    void debug(CharSequence message);

    /**
     * Logs message received event and returns true if the message event is considered spam.
     * @param event Discord message received event.
     * @return True if the message is considered a spam.
     */
    boolean logEvent(MessageReceivedEvent event);
    void logException(CharSequence message, Throwable e);
}
