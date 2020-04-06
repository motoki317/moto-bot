package log;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public interface Logger {
    void log(int botLogCh, CharSequence message);
    void debug(CharSequence message);

    /**
     * Logs message received event.
     * @param event  Discord message received event.
     * @param isSpam If the message was considered spam.
     */
    void logEvent(MessageReceivedEvent event, boolean isSpam);
    void logException(CharSequence message, Throwable e);
}
