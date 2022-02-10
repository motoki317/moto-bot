package log;

import commands.event.CommandEvent;

public interface Logger {
    void log(int botLogCh, CharSequence message);
    void debug(CharSequence message);

    /**
     * Logs message received event.
     * @param event  Discord message received event.
     * @param isSpam If the message was considered spam.
     */
    void logEvent(CommandEvent event, boolean isSpam);
    void logException(CharSequence message, Throwable e);
}
